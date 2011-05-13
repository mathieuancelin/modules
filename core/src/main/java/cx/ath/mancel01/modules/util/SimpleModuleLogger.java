package cx.ath.mancel01.modules.util;

import java.io.PrintStream;

public class SimpleModuleLogger {

    private static boolean trace = false;

    public static void enableTrace(boolean trace) {
        SimpleModuleLogger.trace = trace;
    }

    public static void error(String message, Object... printable) {
        print(System.err, message, printable);
    }

    public static void info(String message, Object... printable) {
        print(System.out, message, printable);
    }

    public static void trace(String message, Object... printable) {
        if (trace) {
            print(System.out, "[TRACE] " + message, printable);
        }
    }

    private static void print(PrintStream out, String message, Object... printable) {
        StringBuilder print = new StringBuilder();
        if (message.contains("{}") && printable != null && printable.length > 0) {
            String[] parts = message.split("\\{\\}");
            int i = 0;
            for (String part : parts) {
                print.append(part);
                if (i < printable.length) {
                    print.append(printable[i]);
                    i++;
                }
            }
            if (i < printable.length) {
                for (int j = i; j < printable.length; j++) {
                    print.append(printable[j]);
                    print.append("\n");
                }
            }
        } else {
            print.append(message);
            if (printable != null && printable.length > 0) {
                print.append("\n");
            }
            for (Object o : printable) {
                print.append(o.toString());
                print.append("\n");
            }
        }
        out.println(print.toString());
    }
}
