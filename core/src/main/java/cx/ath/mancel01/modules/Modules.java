package cx.ath.mancel01.modules;

import cx.ath.mancel01.modules.module.ClassPathModuleImpl;
import cx.ath.mancel01.modules.api.Configuration;
import cx.ath.mancel01.modules.exception.AddModuleException;
import cx.ath.mancel01.modules.exception.ModuleNotFoundException;
import cx.ath.mancel01.modules.exception.ModuleNotReadableException;
import cx.ath.mancel01.modules.module.Module;
import cx.ath.mancel01.modules.util.Configurations.ComplexConfigurationFromNonModularJar;
import cx.ath.mancel01.modules.util.Configurations.SimpleConfigurationFromNonModularJar;
import cx.ath.mancel01.modules.util.Configurations.StandardModuleConfiguration;
import cx.ath.mancel01.modules.util.SimpleModuleLogger;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Modules {

    public static final ClassPathModuleImpl CLASSPATH_MODULE = new ClassPathModuleImpl();

    // TODO : make it pluggable
    private static final Map<String, Modules> availablePlatforms = new HashMap<String, Modules>();

    private Map<String, Module> modules = new HashMap<String, Module>();

    private final String id;

    public Modules() {
        SimpleModuleLogger.info("Creation of a new Java Modules Container !");
        this.id = UUID.randomUUID().toString(); // TODO : make it pluggable
        availablePlatforms.put(id, this);
    }

    public void addModule(final Configuration configuration) {
        Module module = new Module(configuration, this);
        module.validate();
        modules.put(module.identifier, module);
    }

    public void addModules(final Collection<Configuration> configurations) {
        addModules(configurations.toArray(new Configuration[configurations.size()]));
    }

    public void addModules(final Configuration... configurations) {
        List<Module> newModules = new ArrayList<Module>();
        for (Configuration configuration : configurations) {
            Module module = new Module(configuration, this);
            newModules.add(module);
            modules.put(module.identifier, module);
        }
        try {
            for (Module module : newModules) {
                module.validate();
            }
        } catch (Exception ex) {
            for (Module module : newModules) {
                modules.remove(module.identifier);
            }
            throw new AddModuleException(ex);
        }
    }

    public void removeModule(final String identifier) {
        if (modules.containsKey(identifier)) {
            modules.remove(identifier);
        } else {
            throw new ModuleNotFoundException("Module " + identifier + " not found");
        }
    }

    public Module getModule(final String identifier) {
        if (modules.containsKey(identifier)) {
            return modules.get(identifier);
        } else {
            throw new ModuleNotFoundException("Module " + identifier + " not found");
        }
    }

    public void clear() {
        modules.clear();
    }

    public void startModule(String identifier) {
        if (modules.containsKey(identifier)) {
            modules.get(identifier).start();
        } else {
            throw new ModuleNotFoundException("Module " + identifier + " not found");
        }
    }

    public Map<String, Module> getModules() {
        return modules;
    }

    public String id() {
        return id;
    }

    public static Modules getModules(final String id) {
        return availablePlatforms.get(id);
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
            Configuration configuration = 
                    new StandardModuleConfiguration(
                        name, version, mainClass, dependencies, jar);
            file.close();
            return configuration;
        } catch (Exception ex) {
            throw new ModuleNotReadableException("Can't read module", ex);
        }
    }

    private static Configuration getConfigurationFromNonModularJar(final URL jar) {
        return new SimpleConfigurationFromNonModularJar(jar);
    }

    public static Configuration getConfigurationFromNonModularJar(final URL jar,
            final String name, final String version) {
        return new ComplexConfigurationFromNonModularJar(jar, version, name);
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("\nModules container ").append(id).append("\n");
        b.append("Available modules : \n\n");
        for (Module m : modules.values()) {
            b.append(m.identifier);
            b.append("\n");
        }
        return b.toString();
    }
}
