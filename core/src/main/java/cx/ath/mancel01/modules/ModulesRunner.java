package cx.ath.mancel01.modules;

import cx.ath.mancel01.modules.api.Configuration;
import cx.ath.mancel01.modules.util.SimpleModuleLogger;
import cx.ath.mancel01.modules.util.URLUtils;
import java.io.File;
import java.util.Collection;

public class ModulesRunner {

    public static void main(String... args) {
        if (args != null && args.length > 0) {
            int length = args.length;
            if (length != 1 && length != 2 && length != 3 && length != 4) {
                usage();
                System.exit(-1);
            } else {
                String path = ".";
                String run = null;
                if (length == 1) {
                    run = args[0];
                }
                if (length == 2) {
                    SimpleModuleLogger.enableTrace(true);
                    if (!args[1].equals("-trace")) {
                        usage();
                        System.exit(-1);
                    }
                    run = args[0];
                }
                if (length == 3) {
                    if (args[0].equals("-mp")) {
                        path = args[1];
                        run = args[2];
                    } else {
                        if (!args[1].equals("-mp")) {
                            usage();
                            System.exit(-1);
                        }
                        run = args[0];
                        path = args[2];
                    }
                }
                if (length == 4) {
                    SimpleModuleLogger.enableTrace(true);
                    if (!args[3].equals("-trace")) {
                        usage();
                        System.exit(-1);
                    }
                    if (args[0].equals("-mp")) {
                        path = args[1];
                        run = args[2];
                    } else {
                        if (!args[1].equals("-mp")) {
                            usage();
                            System.exit(-1);
                        }
                        run = args[0];
                        path = args[2];
                    }
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
            }
        } else {
            usage();
            System.exit(-1);
        }
    }

    public static void usage() {
        System.out.println("Java Modules version 1.0\n");
        System.out.println("Modules can be used with the following commands :\n");
        System.out.println("java -jar modules.jar com.sample.module1:1.0");
        System.out.println("java -jar modules.jar com.sample.module1:1.0 -trace");
        System.out.println("java -jar modules.jar com.sample.module1:1.0 -mp /tmp");
        System.out.println("java -jar modules.jar com.sample.module1:1.0 -mp /tmp -trace");
        System.out.println("java -jar modules.jar -mp /tmp com.sample.module1:1.0");
        System.out.println("java -jar modules.jar -mp /tmp com.sample.module1:1.0 -trace");
        System.out.println("");
    }
}
