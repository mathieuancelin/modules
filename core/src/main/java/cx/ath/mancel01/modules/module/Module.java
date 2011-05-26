package cx.ath.mancel01.modules.module;

import cx.ath.mancel01.modules.api.Configuration;
import cx.ath.mancel01.modules.Modules;
import cx.ath.mancel01.modules.api.Dependency;
import cx.ath.mancel01.modules.api.ModuleClassLoader;
import cx.ath.mancel01.modules.exception.MissingDependenciesException;
import cx.ath.mancel01.modules.exception.ModuleStartupException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

public class Module {

    public static final String VERSION_SEPARATOR = ":";

    private static final String MAIN_METHOD = "main";
    
    public final String identifier;
    public final String name;
    public final String version;

    private final Collection<Dependency> dependencies;
    private final List<String> allTransitiveManagedClasses;
    private final Modules delegateModules;
    private final Configuration configuration;

    final Set<String> areCircular;
    
    private ModuleClassLoaderImpl moduleClassloader;

    public Module(final Configuration configuration, Modules modules) {
        this.identifier = configuration.name() + VERSION_SEPARATOR + configuration.version();
        this.name = configuration.name();
        this.version = configuration.version();
        this.dependencies = configuration.dependencies();
        this.configuration = configuration;
        this.delegateModules = modules;
        this.areCircular = new HashSet<String>();
        this.allTransitiveManagedClasses = new ArrayList<String>();
        if (configuration.rootResource() != null) {
            this.moduleClassloader = 
                new ModuleClassLoaderImpl(
                    new URL[]{configuration.rootResource()}, this);
        }
    }

    public void start() {
        if (configuration.startable()) {
            ClassLoader old = Thread.currentThread().getContextClassLoader();
            try {
                final Class<?> main = load(configuration.mainClass());
                Method mainMethod = main.getMethod(MAIN_METHOD, String[].class);
                final int modifiers = mainMethod.getModifiers();
                if (!Modifier.isStatic(modifiers)) {
                    throw new NoSuchMethodException("Main method is not static for " + this);
                }
                Object arg = new String[] {};
                Thread.currentThread().setContextClassLoader(moduleClassloader);
                mainMethod.invoke(null, arg);
            } catch (Exception ex) {
                throw new ModuleStartupException(ex);
            } finally {
                Thread.currentThread().setContextClassLoader(old);
            }
        }
    }

    public Class<?> load(String name) {
        try {
            return moduleClassloader.loadClass(name);
        } catch (ClassNotFoundException ex) {
            throw new MissingDependenciesException("Missing dependency for class "
                    + name + "from module " + identifier);
        }
    }

    public void validate() {
        List<String> missing = new ArrayList<String>();
        for (Dependency dependency : dependencies) {
            if (!delegateModules.getModules().containsKey(dependency)) {
                missing.add("Missing dependency : " + dependency.identifier());
            }
        }
        if (!missing.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            for (String miss : missing) {
                builder.append(miss);
                builder.append("\n");
            }
            throw new MissingDependenciesException("Missing dependencies for module "
                    + identifier + " :\n" + builder.toString());
        }
        List<String> marked = new ArrayList<String>();
        DependencyImpl
            .checkForCircularDependencies(
                marked, this, delegateModules, areCircular);
    }

    public Enumeration<URL> getResources(String name) throws IOException {
        Enumeration<URL> urls = moduleClassloader.getJarResources(name);
        Set<URL> externalUrls = new HashSet<URL>();
        if (urls != null) {
            externalUrls.addAll(Collections.list(urls));
        }
        for (Module mod : delegateModules.getModules().values()) {
            Enumeration<URL> tmp = mod.moduleClassloader.getJarResources(name);
            if (tmp != null) {
                externalUrls.addAll(Collections.list(tmp));
            }
        }
        return Collections.enumeration(externalUrls);
    }


    public InputStream getResourceAsStream(String name) {
        InputStream is = moduleClassloader.getJarResourceAsStream(name);
        if (is == null) {
            Set<InputStream> iss = new HashSet<InputStream>();
            for (Module mod : delegateModules.getModules().values()) {
                InputStream tmp = mod.moduleClassloader.getJarResourceAsStream(name);
                if (tmp != null) {
                    iss.add(mod.getResourceAsStream(name));
                }
            }
            for (InputStream i : iss) {
                return i;
            }
            return null;
        } else {
            return is;
        }
    }

    public URL getResource(String name) {
        URL url = moduleClassloader.getJarResource(name);
        if (url == null) {
            Set<URL> urls = new HashSet<URL>();
            for (Module mod : delegateModules.getModules().values()) {
                URL tmp = mod.moduleClassloader.getJarResource(name);
                if (tmp != null) {
                    urls.add(mod.getResource(name));
                }
            }
            for (URL u : urls) {
                return u;
            }
            return null;
        } else {
            return url;
        }
    }

    public List<String> getAllManagedClasses() {
        Set<String> visited = new HashSet<String>();
        visited.add(identifier);
        return getAllTransitiveManagedClasses(visited);
    }

    public void computeDependencies() {
        Set<String> visited = new HashSet<String>();
        visited.add(this.identifier);
        // compute transitive dependencies
        computeTransitiveManagedClasses(visited);
        // compute direct dependencies
        computeDirectManagedClasses();
    }

    public boolean canLoad(String name) {
        return moduleClassloader.canLoad(name);
    }

    public Collection<Dependency> dependencies() {
        return dependencies;
    }

    public <T> ServiceLoader<T> load(Class<T> clazz) {
        return ServiceLoader.load(clazz, moduleClassloader);
    }

    public Configuration configuration() {
        return configuration;
    }

    private Modules delegateModules() {
        return delegateModules;
    }

    private ModuleClassLoaderImpl getModuleClassloader() {
        return moduleClassloader;
    }

    private List<String> getManagedClasses() {
        return moduleClassloader.getManagedClasses();
    }

    private void computeDirectManagedClasses() {
        for (Dependency dep : this.dependencies()) {
            Module m = this.delegateModules().getModule(dep.identifier());
            for (String clazz : m.getManagedClasses()) {
                moduleClassloader.getDirectDependenciesManagedClasses()
                    .put(clazz, m.getModuleClassloader());
            }
        }
    }

    private List<String> getAllTransitiveManagedClasses(Set<String> visited) {
        // assuming dependencies are already computed
        if (depsHasChanged()) {
            allTransitiveManagedClasses.clear();
            allTransitiveManagedClasses.addAll(moduleClassloader.getManagedClasses());
            allTransitiveManagedClasses.addAll(moduleClassloader
                    .getDependenciesManagedClasses().keySet());
        }
        return allTransitiveManagedClasses;
    }

    /**
     * Dumb method to know if we need to recalculate transitive dependencies
     */
    private boolean depsHasChanged() {
        int cache = allTransitiveManagedClasses.size();
        int direct = moduleClassloader.getManagedClasses().size();
        int deps = moduleClassloader.getDependenciesManagedClasses().size();
        return (cache != (direct + deps));
    }

    private void computeTransitiveManagedClasses(Set<String> visited) {
        for (Dependency dep : this.dependencies()) {
            Module m = this.delegateModules().getModule(dep.identifier());
            if (!visited.contains(dep.identifier())) {
                visited.add(dep.identifier());
                for (String clazz : m.getAllTransitiveManagedClasses(visited)) {
                    moduleClassloader.getDependenciesManagedClasses()
                            .put(clazz, m.getModuleClassloader());
                }
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Module other = (Module) obj;
        if ((this.identifier == null) ? (other.identifier != null)
                : !this.identifier.equals(other.identifier)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + (this.identifier != null ? this.identifier.hashCode() : 0);
        return hash;
    }

    public static String getIdentifier(String name, String version) {
        return name + Module.VERSION_SEPARATOR + version;
    }

    public static <T> ServiceLoader<T> load(Module module, Class<T> clazz) {
        return module.load(clazz);
    }

    public static Module getModule(Class<?> clazz) {
        ClassLoader loader = clazz.getClassLoader();
        if (ModuleClassLoader.class.isAssignableFrom(loader.getClass())) {
            ModuleClassLoader classLoader = (ModuleClassLoader) loader;
            return classLoader.getModule();
        }
        return Modules.CLASSPATH_MODULE;
    }

    public static Module getModule(ClassLoader loader) {
        if (ModuleClassLoader.class.isAssignableFrom(loader.getClass())) {
            ModuleClassLoader classLoader = (ModuleClassLoader) loader;
            return classLoader.getModule();
        }
        return Modules.CLASSPATH_MODULE;
    }
}
