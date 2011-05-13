package cx.ath.mancel01.modules;

import cx.ath.mancel01.modules.module.ClassPathModuleImpl;
import cx.ath.mancel01.modules.api.Configuration;
import cx.ath.mancel01.modules.module.Module;
import cx.ath.mancel01.modules.util.SimpleModuleLogger;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Modules {

    public static final ClassPathModuleImpl CLASSPATH_MODULE = new ClassPathModuleImpl();

    private Map<String, Module> modules = new HashMap<String, Module>();

    public Modules() {
        SimpleModuleLogger.info("Creation of a new Java Modules Container !");
    }

    public void addModule(final Configuration configuration) {
        Module module = new Module(configuration, this);
        module.validate();
        modules.put(configuration.name()
                + Module.VERSION_SEPARATOR + configuration.version(),
                module);
    }

    public void addModules(final Collection<Configuration> configurations) {
        addModules(configurations.toArray(new Configuration[configurations.size()]));
    }

    public void addModules(final Configuration... configurations) {
        List<Module> newModules = new ArrayList<Module>();
        for (Configuration configuration : configurations) {
            Module module = new Module(configuration, this);
            newModules.add(module);
            modules.put(configuration.name()
                + Module.VERSION_SEPARATOR + configuration.version(), module);
        }
        try {
            for (Module module : newModules) {
                module.validate();
            }
        } catch (Exception ex) {
            for (Module module : newModules) {
                modules.remove(module.identifier);
            }
            throw new RuntimeException(ex);
        }
    }

    public void removeModule(final String identifier) {
        if (modules.containsKey(identifier)) {
            modules.remove(identifier);
        } else {
            throw new IllegalStateException("Module " + identifier + " not found");
        }
    }

    public Module getModule(final String identifier) {
        if (modules.containsKey(identifier)) {
            return modules.get(identifier);
        } else {
            throw new IllegalStateException("Module " + identifier + " not found");
        }
    }

    public void clear() {
        modules.clear();
    }

    public void startModule(String identifier) {
        if (modules.containsKey(identifier)) {
            modules.get(identifier).start();
        } else {
            throw new IllegalStateException("Module " + identifier + " not found");
        }
    }

    public Map<String, Module> getModules() {
        return modules;
    }

    public static Collection<Configuration> scanForModules(final URL dir) {
        Set<Configuration> modulesConfig = new HashSet<Configuration>();
        File root = new File(dir.getFile());
        Set<File> modules = new HashSet<File>();
        scanChildren(root, modules);
        for (File file : modules) {
            try {
                Configuration config = getConfigurationFromJar(file.toURI().toURL());
                if (config != null) {
                    modulesConfig.add(config);
                }
            } catch (Exception ex) {
                SimpleModuleLogger.error("Ignore file {}", file.getAbsolutePath());
            }
        }
        return modulesConfig;
    }

    private static void scanChildren(File root, Set<File> found) {
        File[] children = root.listFiles();
        if (children != null && children.length > 0) {
            for (File file : children) {
                if (file.getName().endsWith(".jar")) {
                    found.add(file);
                }
                if (file.isDirectory()) {
                    scanChildren(file, found);
                }
            }
        }
    }

    public static Configuration getConfigurationFromJar(final URL jar) {
        String fileName = jar.getFile();
        if (!fileName.endsWith(".jar")) {
            throw new IllegalStateException("File isn't a jar : " + jar);
        }
        try {
            ZipFile file = new ZipFile(new File(fileName));
            ZipEntry conf = file.getEntry("META-INF/configuration.properties");
            if (conf == null) {
                return getConfigurationFromNonModularJar(jar);
            }
            Properties p = new Properties();
            p.load(file.getInputStream(conf));
            final String name = p.getProperty("name");
            final String version = p.getProperty("version");
            final String mainClass = p.getProperty("main");
            final String dependencies = p.getProperty("dependencies");
            Configuration configuration = new Configuration() {
                @Override
                public String name() { return name; }
                @Override
                public Collection<String> dependencies() {
                    if (dependencies == null) {
                        return Collections.emptyList();
                    }
                    return Arrays.asList(dependencies.split(";"));
                }
                @Override
                public Collection<String> optionalDependencies() {
                    return Collections.emptyList();
                }
                @Override
                public boolean startable() {
                    return (mainClass != null);
                }
                @Override
                public String mainClass() {
                    return mainClass;
                }
                @Override
                public URL rootResource() { return jar; }

                @Override
                public String version() {
                    if (version == null) {
                        return "1.0";
                    }
                    return version;
                }

                @Override
                public String identifier() {
                    return name() + Module.VERSION_SEPARATOR + version();
                }
            };
            return configuration;
        } catch (Exception ex) {
            throw new IllegalStateException("Can't read module", ex);
        }
    }

    private static Configuration getConfigurationFromNonModularJar(final URL jar) {
        return new Configuration() {

            private final String name = new File(jar.getFile()).getName();

            @Override
            public String name() {
                return name;
            }

            @Override
            public String version() {
                return "";
            }

            @Override
            public String identifier() {
                return name;
            }

            @Override
            public Collection<String> dependencies() {
                return Collections.emptyList();
            }

            @Override
            public Collection<String> optionalDependencies() {
                return Collections.emptyList();
            }

            @Override
            public boolean startable() {
                return false;
            }

            @Override
            public String mainClass() {
                return null;
            }

            @Override
            public URL rootResource() {
                return jar;
            }
        };
    }

    public static Configuration getConfigurationFromNonModularJar(final URL jar,
            final String name, final String version) {
        return new Configuration() {

            @Override
            public String name() {
                return name;
            }

            @Override
            public String version() {
                return version;
            }

            @Override
            public String identifier() {
                return name + Module.VERSION_SEPARATOR + version;
            }

            @Override
            public Collection<String> dependencies() {
                return Collections.emptyList();
            }

            @Override
            public Collection<String> optionalDependencies() {
                return Collections.emptyList();
            }

            @Override
            public boolean startable() {
                return false;
            }

            @Override
            public String mainClass() {
                return null;
            }

            @Override
            public URL rootResource() {
                return jar;
            }
        };
    }
}
