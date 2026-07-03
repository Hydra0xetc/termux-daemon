package org.termux.daemon;

public class Logger {
    private static Logger instance;

    public enum LogLevel {
        INFO, DEBUG, WARN, ERROR
    }

    private Logger() {}

    public static Logger getInstance() {
        if (instance == null) {
            return new Logger();
        }

        return instance;

    }

    public void log(LogLevel level, String tag, String msg) {
        String prefix = switch (level) {
            case INFO  -> "[INFO]";
            case DEBUG -> "[DEBUG]";
            case WARN  -> "[WARN]";
            case ERROR -> "[ERROR]";
        };

        System.out.printf("%s [%s] %s\n", prefix, tag, msg);
    }
}
