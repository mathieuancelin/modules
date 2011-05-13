package cx.ath.mancel01.modules.module;

import cx.ath.mancel01.modules.api.Configuration;
import cx.ath.mancel01.modules.Modules;
import cx.ath.mancel01.modules.api.ModuleClassLoader;
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
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Module {

    public static final String VERSION_SEPARATOR = ":";
    private static final Logger logger = LoggerFactory.getLogger(Module.class);
    public final String identifier;
    public final String name;
    public final String version;
    private ModuleClassLoaderImpl moduleClassloader;
    private final Collection<String> dependencies;
    private final Modules delegateModules;
    private final Configuration configuration;
    private ThreadLocal<List<String>> markedTL = new ThreadLocal<List<String>>();

    public Module(final Configuration configuration, Modules modules) {
        this.identifier = configuration.name() + VERSION_SEPARATOR + configuration.version();
        this.name = configuration.name();
        this.version = configuration.version();
        this.dependencies = configuration.dependencies();
        this.configuration = configuration;
        this.delegateModules = modules;
        if (configuration.rootResource() != null) {
            this.moduleClassloader = new ModuleClassLoaderImpl(new URL[]{configuration.rootResource()}, this);
        }
    }

    public void start() {
        if (configuration.startable()) {
            try {
                final Class<?> main = load(configuration.mainClass());
                Method mainMethod = main.getMethod("main", String[].class);
                final int modifiers = mainMethod.getModifiers();
                if (!Modifier.isStatic(modifiers)) {
                    throw new NoSuchMethodException("Main method is not static for " + this);
                }
                Object arg = new String[] {};
                mainMethod.invoke(null, arg);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    public Class<?> load(String name) {
        try {
            return moduleClassloader.loadClass(name);
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("Missing dependency for class "
                    + name + "from module " + identifier);
        }
    }

    public boolean canLoad(String name) {
        return moduleClassloader.canLoad(name);
    }

    public Collection<String> dependencies() {
        return dependencies;
    }

    public void validate() {
        List<String> missing = new ArrayList<String>();
        for (String dependency : dependencies) {
            if (!delegateModules.getModules().containsKey(dependency)) {
                missing.add("Missing dependency : " + dependency);
            }
        }
        if (!missing.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            for (String miss : missing) {
                builder.append(miss);
                builder.append("\n");
            }
            throw new IllegalStateException("Missing dependencies for module "
                    + identifier + " :\n" + builder.toString());
        }
    }

    Modules delegateModules() {
        return delegateModules;
    }

    Configuration configuration() {
        return configuration;
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
        if ((this.identifier == null) ? (other.identifier != null) : !this.identifier.equals(other.identifier)) {
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

    public List<String> getAllLoadableClasses() {
        Set<String> visited = new HashSet<String>();
        visited.add(identifier);
        return getAllLoadableClasses(visited);
    }

    List<String> getAllLoadableClasses(Set<String> visited) {
        return moduleClassloader.getAllManagedClasses(visited);
    }

    List<String> getManagedClasses() {
        return moduleClassloader.getManagedClasses();
    }

    public static String getIdentifier(String name, String version) {
        return name + Module.VERSION_SEPARATOR + version;
    }

    public static Module getModule(Class<?> clazz) {
        ClassLoader loader = clazz.getClassLoader();
        if (ModuleClassLoader.class.isAssignableFrom(loader.getClass())) {
            ModuleClassLoader classLoader = (ModuleClassLoader) loader;
            return classLoader.getModule();
        }
        return Modules.CLASSPATH_MODULE;
    }
}
