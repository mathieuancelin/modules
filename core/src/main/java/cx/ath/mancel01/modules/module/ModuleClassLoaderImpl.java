package cx.ath.mancel01.modules.module;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import cx.ath.mancel01.modules.Modules;
import cx.ath.mancel01.modules.api.ModuleClassLoader;
import cx.ath.mancel01.modules.api.Resource;
import java.io.File;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModuleClassLoaderImpl extends URLClassLoader implements ModuleClassLoader {

    private static final Logger logger = LoggerFactory.getLogger(ModuleClassLoaderImpl.class);

    public static final List<String> bootDelegationList;

    private final Module module;

    private final List<String> managedClasses;

    private final Set<String> dependenciesManagedClasses;
    
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
        dependenciesManagedClasses = new HashSet<String>();
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
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        for (String pack : bootDelegationList) {
            if (name.startsWith(pack)) {
                logger.debug("Delegating {} to {}", name, Modules.CLASSPATH_MODULE.identifier);
                return Modules.CLASSPATH_MODULE.load(name);
            }
        }
        boolean err = false;
        try {
            return super.loadClass(name);
        } catch (Throwable t) {
            err = true;
            List<String> marked = Collections.synchronizedList(new ArrayList<String>());
            for (String dependency : module.dependencies()) {
                if (module.delegateModules().getModules().containsKey(dependency)) {
                    Module dep = module.delegateModules().getModules().get(dependency);
                    if (!marked.contains(dep.identifier)) {
                        marked.add(dep.identifier);
                        if (dep.canLoad(name)) {
                            logger.debug("Delegating {} to {}", name, dep.identifier);
                            return dep.load(name);
                        }
                    }
                }
            }
            throw new IllegalStateException("Missing dependency for class " + name + " from module " + module.identifier);
        } finally {
            if (!err) {
                logger.debug("Loading {} from {}", name, module.identifier);
            }
        }
    }

    public boolean canLoad(String name) {
        for (String className : managedClasses) {
            if (className.equals(name)) {
                return true;
            }
        }
        if (dependenciesManagedClasses.isEmpty()) {
            Set<String> visited = new HashSet<String>();
            visited.add(module.identifier);
            computeDependenciesLoadable(visited);
        }
        for (String className : dependenciesManagedClasses) {
            if (className.equals(name)) {
                return true;
            }
        }
        return false;
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
        classes.addAll(dependenciesManagedClasses);
        return classes;
    }

    private void computeDependenciesLoadable(Set<String> visited) {
        for (String dep : module.dependencies()) {
            Module m = module.delegateModules().getModule(dep);
            if (!visited.contains(dep)) {
                visited.add(dep);
                dependenciesManagedClasses.addAll(m.getAllLoadableClasses(visited));
            }
        }
    }
}
