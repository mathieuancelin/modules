package cx.ath.mancel01.modules.module;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import cx.ath.mancel01.modules.Modules;
import cx.ath.mancel01.modules.api.Dependency;
import cx.ath.mancel01.modules.api.ModuleClassLoader;
import cx.ath.mancel01.modules.api.Resource;
import cx.ath.mancel01.modules.exception.MissingDependenciesException;
import cx.ath.mancel01.modules.exception.ModuleClassloaderCreationException;
import cx.ath.mancel01.modules.util.SimpleModuleLogger;
import java.io.File;

import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ModuleClassLoaderImpl extends URLClassLoader implements ModuleClassLoader {

    public static final List<String> bootDelegationList;

    private final Module module;

    private final List<String> managedClasses;

    private final Map<String, ModuleClassLoaderImpl> dependenciesManagedClasses;

    private final List<Resource> managedResources;

    static {
        bootDelegationList = new ArrayList<String>();
        bootDelegationList.add("java.");
        bootDelegationList.add("javax.");
        bootDelegationList.add("org.w3c.dom");
        bootDelegationList.add("sun.");
        bootDelegationList.add("sunw.");
        bootDelegationList.add("com.sun.");
    }

    public ModuleClassLoaderImpl(URL[] urls, Module from) {
        super(urls, Modules.CLASSPATH_MODULE.getLoader());
        this.module = from;
        managedClasses = new ArrayList<String>();
        dependenciesManagedClasses = new HashMap<String, ModuleClassLoaderImpl>();
        managedResources = new ArrayList<Resource>();
        final URL root = module.configuration().rootResource();
        String fileName = root.getFile();
        try {
            final ZipFile file = new ZipFile(new File(fileName));
            Enumeration entries = file.entries();
            while (entries.hasMoreElements()) {
                final ZipEntry entry = (ZipEntry) entries.nextElement();
                if (entry.getName().endsWith(".class")) {
                    String className = entry.getName().substring(0, entry.getName().lastIndexOf("."));
                    className = className.replace("/", ".").replace("$", ".");
                    managedClasses.add(className);
                } else {
                    managedResources.add(new Resource() {
                        @Override
                        public URL getURL() {
                            try {
                                return new URL(root.toString() + "!" + entry.getName());
                            } catch (MalformedURLException ex) {
                                return null;
                            }
                        }
                        @Override
                        public InputStream getInsputStream() {
                            try {
                                return file.getInputStream(entry);
                            } catch (IOException ex) {
                                return null;
                            }
                        }
                        @Override
                        public String getName() {
                            return entry.getName();
                        }
                    });
                }
            }
        } catch (Exception ex) {
            throw new ModuleClassloaderCreationException(ex);
        }
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        long start = System.nanoTime();
        boolean dontShow = false;
        Class<?> loadedClass = findLoadedClass(name);
        if (loadedClass != null) {
            dontShow = true;
            return loadedClass;
        }
        for (String pack : bootDelegationList) {
            if (name.startsWith(pack)) {
                SimpleModuleLogger.trace("Delegating {} to {}",
                    name, Modules.CLASSPATH_MODULE.identifier);
                return Modules.CLASSPATH_MODULE.load(name);
            }
        }
        try {
            if (managedClasses.contains(name)) {
                return super.loadClass(name);
            } else {
                dontShow = true;
                return delegate(name);
            }
        } catch (Throwable t) {
            dontShow = true;
            return delegate(name);
        } finally {
            if (!dontShow) {
                SimpleModuleLogger.trace("Loading {} from {} in {} ms", name,
                    module.identifier, new SimpleModuleLogger.Duration(start, System.nanoTime()));
            }
        }
    }

    private Class<?> delegate(String name) throws ClassNotFoundException {
        try {
            if (dependenciesManagedClasses.containsKey(name)) {
                ModuleClassLoaderImpl dep = dependenciesManagedClasses.get(name);
                SimpleModuleLogger.trace("Delegating {} to {}",
                    name, dep.module.identifier);
                return dep.loadClass(name);
            }
            throw new MissingDependenciesException("Missing dependency for class "
                + name + " from module " + module.identifier);
        } catch (Throwable t) {
            throw new MissingDependenciesException("Missing dependency for class "
                + name + " from module " + module.identifier);
        }
//        for (Dependency dependency : module.dependencies()) {
//            if (module.delegateModules().getModules().containsKey(dependency)) {
//                Module dep = module.delegateModules().getModules().get(dependency);
//                if (dep.canLoad(name)) {
//                    SimpleModuleLogger.trace("Delegating {} to {} in {} ms",
//                        name, dep.identifier, new SimpleModuleLogger.Duration(start, System.nanoTime()));
//                    return dep.load(name);
//                }
//            }
//        }
    }

    public boolean canLoad(String name) {
        for (String className : managedClasses) {
            if (className.equals(name)) {
                return true;
            }
        }
        if (dependenciesManagedClasses.isEmpty()) {
            computeDependencies(); // should be already computed at install time
        }
        for (String className : dependenciesManagedClasses.keySet()) { // TODO : manage exports
            if (className.equals(name)) {
                return true;
            }
        }
        return false;
    }

    public void computeDependencies() {
        Set<String> visited = new HashSet<String>();
        visited.add(module.identifier);
        computeDependenciesLoadable(visited);
    }

    List<String> getManagedClasses() {
        return managedClasses;
    }

    @Override
    public Module getModule() {
        return module;
    }

    @Override
    public URL getResource(String name) {
        return module.getResource(name);
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        return module.getResourceAsStream(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        return module.getResources(name);
    }

    URL getJarResource(String name) {
        return super.getResource(name);
    }

    InputStream getJarResourceAsStream(String name) {
        return super.getResourceAsStream(name);
    }

    Enumeration<URL> getJarResources(String name) throws IOException {
        return super.getResources(name);
    }

    List<String> getAllManagedClasses(Set<String> visited) {
        if (dependenciesManagedClasses.isEmpty()) {
            visited.add(module.identifier);
            computeDependenciesLoadable(visited);
        }
        List<String> classes = new ArrayList<String>();
        classes.addAll(managedClasses);
        classes.addAll(dependenciesManagedClasses.keySet());
        return classes;
    }

    private void computeDependenciesLoadable(Set<String> visited) {
        for (Dependency dep : module.dependencies()) {
            Module m = module.delegateModules().getModule(dep.identifier());
            if (!visited.contains(dep.identifier())) {
                visited.add(dep.identifier());
                for (String clazz : m.getAllLoadableClasses(visited)) {
                    dependenciesManagedClasses.put(clazz, m.getModuleClassloader());
                }
            }
        }
    }
}
