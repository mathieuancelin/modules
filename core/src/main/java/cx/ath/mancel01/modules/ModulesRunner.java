package cx.ath.mancel01.modules;

import cx.ath.mancel01.modules.api.Configuration;
import cx.ath.mancel01.modules.util.SimpleModuleLogger;
import cx.ath.mancel01.modules.util.URLUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ModulesRunner {

    public static final String TRACE = "-trace";

    public static final String COLORS = "-colors";
    
    public static final String MODULES_PATH = "-mp";

    public static void main(String... args) {
        if (args != null && args.length > 0) {
            List<String> argList = new ArrayList<String>();
            for (String arg : args) {
                argList.add(arg);
            }
            String path = ".";
            String run = null;
            if (argList.contains(TRACE)) {
                SimpleModuleLogger.enableTrace(true);
                argList.remove(TRACE);
            }
            if (argList.contains(COLORS)) {
                SimpleModuleLogger.enableColors(true);
                argList.remove(COLORS);
            }
            if (argList.contains(MODULES_PATH)) {
                int index = argList.indexOf(MODULES_PATH);
                try {
                    path = argList.get(index + 1);
                    argList.remove(path);
                    argList.remove(MODULES_PATH);
                } catch (Exception e) {
                    usage();
                    System.exit(-1);
                }
            }
            if (argList.isEmpty()) {
                usage();
                System.exit(-1);
            } else {
                run = argList.get(0);
            }
            File root = new File(path);
            SimpleModuleLogger.info("\nScanning {} ...", root.getAbsolutePath());
            long start = System.currentTimeMillis();
            Collection<Configuration> configs =
                    Modules.scanForModules(URLUtils.url(path));
            SimpleModuleLogger.info("Found {} module(s) in {} ms\n",
                    configs.size(),
                    String.valueOf((System.currentTimeMillis() - start)));
            Modules modules = new Modules();
            modules.addModules(configs);
            modules.startModule(run);
        } else {
            usage();
            System.exit(-1);
        }
    }

    public static void usage() {
        SimpleModuleLogger.info("Java Modules version 1.0\n");
        SimpleModuleLogger.info("Modules can be used with the following commands :\n");
        SimpleModuleLogger.info("java -jar modules.jar com.sample.module1:1.0");
        SimpleModuleLogger.info("java -jar modules.jar com.sample.module1:1.0 {}", TRACE);
        SimpleModuleLogger.info("java -jar modules.jar com.sample.module1:1.0 {} /tmp", MODULES_PATH);
        SimpleModuleLogger.info("java -jar modules.jar com.sample.module1:1.0 {} /tmp {}", MODULES_PATH, TRACE);
        SimpleModuleLogger.info("java -jar modules.jar {} /tmp com.sample.module1:1.0", MODULES_PATH);
        SimpleModuleLogger.info("java -jar modules.jar {} /tmp com.sample.module1:1.0 {}", MODULES_PATH, TRACE);
        SimpleModuleLogger.info("");
    }
}
