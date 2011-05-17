package cx.ath.mancel01.modules.util;

import java.io.PrintStream;

public class SimpleModuleLogger {

    private static final int NORMAL = 0;
    private static final int BRIGHT = 1;
    private static final int FOREGROUND_BLACK = 30;
    private static final int FOREGROUND_RED = 31;
    private static final int FOREGROUND_GREEN = 32;
    private static final int FOREGROUND_YELLOW = 33;
    private static final int FOREGROUND_BLUE = 34;
    private static final int FOREGROUND_MAGENTA = 35;
    private static final int FOREGROUND_CYAN = 36;
    private static final int FOREGROUND_WHITE = 37;
    private static final String PREFIX = "\u001b[";
    private static final String SUFFIX = "m";
    private static final char SEPARATOR = ';';
    private static final String END_COLOUR = PREFIX + SUFFIX;
    private static final String FATAL_COLOUR = PREFIX + BRIGHT + SEPARATOR + FOREGROUND_RED + SUFFIX;
    private static final String ERROR_COLOUR = PREFIX + NORMAL + SEPARATOR + FOREGROUND_RED + SUFFIX;
    private static final String WARN_COLOUR = PREFIX + NORMAL + SEPARATOR + FOREGROUND_YELLOW + SUFFIX;
    private static final String INFO_COLOUR = PREFIX + SUFFIX;
    private static final String DEBUG_COLOUR = PREFIX + NORMAL + SEPARATOR + FOREGROUND_CYAN + SUFFIX;
    private static final String TRACE_COLOUR = PREFIX + NORMAL + SEPARATOR + FOREGROUND_BLUE + SUFFIX;
    private static final String TRACE_PREFIX = "[TRACE] ";

    private static boolean trace = false;
    private static boolean colors = false;

    public static void enableTrace(boolean trace) {
        SimpleModuleLogger.trace = trace;
    }

    public static void enableColors(boolean colors) {
        SimpleModuleLogger.colors = colors;
    }

    public static void error(String message, Object... printable) {
        print(ERROR_COLOUR, System.err, message, printable);
    }

    public static void info(String message, Object... printable) {
        print(INFO_COLOUR, System.out, message, printable);
    }

    public static void trace(String message, Object... printable) {
        if (trace) {
            print(TRACE_COLOUR, System.out, TRACE_PREFIX + message, printable);
        }
    }

    private static void print(String color, PrintStream out, String message, Object... printable) {
        StringBuilder print = new StringBuilder();
        if (colors) {
            print.append(color);
        }
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
        if (colors) {
            print.append(END_COLOUR);
        }
        out.println(print.toString());
    }
}
