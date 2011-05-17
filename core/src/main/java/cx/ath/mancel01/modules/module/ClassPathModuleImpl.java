package cx.ath.mancel01.modules.module;

import cx.ath.mancel01.modules.api.Configuration;
import cx.ath.mancel01.modules.exception.MissingDependenciesException;
import cx.ath.mancel01.modules.util.SimpleModuleLogger;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;

public class ClassPathModuleImpl extends Module {

    public static final String IDENTIFIER = "Delegated-ClassPath-" + System.getProperty("java.specification.name").replace(" ", "-");

    private final ClassLoader loader;

    public ClassPathModuleImpl() {
        super(new Configuration() {

            @Override
            public String name() {
                return IDENTIFIER;
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
                return null;
            }

            @Override
            public String version() {
                return System.getProperty("java.specification.version");
            }

            @Override
            public String identifier() {
                return name() + Module.VERSION_SEPARATOR + version();
            }
        }, null);
        loader =  new CPClassLoader(getClass().getClassLoader(), this);
    }

    @Override
    public boolean canLoad(String name) {
        return true;
    }

    @Override
    public Class<?> load(String name) {
        try {
            return loader.loadClass(name);
        } catch (ClassNotFoundException ex) {
            throw new MissingDependenciesException(ex);
        }
    }

    ClassLoader getLoader() {
        return loader;
    }

    private static class CPClassLoader extends ClassLoader {

        private static Method findLoadedClass;

        static {
            try {
                findLoadedClass = ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class);
            } catch (NoSuchMethodException ex) {
                throw new RuntimeException("Can't find method 'findLoadedClass'");
            } catch (SecurityException ex) {
                throw new RuntimeException("Can't find method 'findLoadedClass'");
            }
        }
        
        private Module module;

        public CPClassLoader(ClassLoader parent, Module module) {
            super(parent);
            this.module = module;
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            try {
                Object[] args = new Object[1];
                args[0] = name;
                findLoadedClass.setAccessible(true);
                Class<?> clz = (Class<?>) findLoadedClass.invoke(getParent(), args);
                findLoadedClass.setAccessible(false);
                if (clz != null) {
                    return clz;
                }
            } catch (Exception ex) {}
            SimpleModuleLogger.trace("Loading {} from {}", name, module.identifier);
            return getParent().loadClass(name);
        }
    }
}
