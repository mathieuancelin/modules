package cx.ath.mancel01.modules.util;

import cx.ath.mancel01.modules.api.Configuration;
import cx.ath.mancel01.modules.module.Module;
import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class Configurations {

    public static class StandardModuleConfiguration implements Configuration {

        private final String name;
        private final String version;
        private final String mainClass;
        private final String dependencies;
        private final URL jar;

        public StandardModuleConfiguration(String name, String version, String mainClass, String dependencies, URL jar) {
            this.name = name;
            this.version = version;
            this.mainClass = mainClass;
            this.dependencies = dependencies;
            this.jar = jar;
        }

        @Override
        public String name() {
            return name;
        }

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
        public URL rootResource() {
            return jar;
        }

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
    }

    public static class ComplexConfigurationFromNonModularJar implements Configuration {

        private final URL jar;
        private final String version;
        private final String name;

        public ComplexConfigurationFromNonModularJar(URL jar, String version, String name) {
            this.jar = jar;
            this.version = version;
            this.name = name;
        }

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
    }

    public static class SimpleConfigurationFromNonModularJar implements Configuration {

        private final URL jar;
        private final String name;

        public SimpleConfigurationFromNonModularJar(URL jar) {
            this.jar = jar;
            name = new File(jar.getFile()).getName();
        }

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
    }
}
