package cx.ath.mancel01.modules.module;

import cx.ath.mancel01.modules.api.Configuration;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClassPathModuleImpl extends Module {

    private static final Logger logger = LoggerFactory.getLogger(ClassPathModuleImpl.class);

    public static final String IDENTIFIER = "Delegated-ClassPath-" + System.getProperty("java.specification.name");

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
            throw new RuntimeException(ex);
        }
    }

    ClassLoader getLoader() {
        return loader;
    }

    private static class CPClassLoader extends ClassLoader {

        private Module module;

        public CPClassLoader(ClassLoader parent, Module module) {
            super(parent);
            this.module = module;
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            Class<?> clz = super.findLoadedClass(name);
            if (clz != null) {
                return clz;
            }
            logger.debug("Loading {} from {}", name, module.identifier);
            return super.loadClass(name);
        }
    }
}
