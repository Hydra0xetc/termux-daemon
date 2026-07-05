package org.termux.daemon;

public class Logger {
  private static Logger instance;

  public enum LogLevel {
    INFO, DEBUG, WARN, ERROR
  }

  private Logger() {}

  public static Logger getInstance() {
    if (instance == null) {
      instance = new Logger();
    }

    return instance;
  }

  private void log(LogLevel level, String tag, String msg) {
    String prefix = switch (level) {
      case INFO  -> "[INFO]";
      case DEBUG -> "[DEBUG]";
      case WARN  -> "[WARN]";
      case ERROR -> "[ERROR]";
    };

    System.out.printf("%s [%s] %s\n", prefix, tag, msg);
  }

  public void d(String tag, String msg) {
    log(LogLevel.DEBUG, tag, msg);
  }

  public void i(String tag, String msg) {
    log(LogLevel.INFO, tag, msg);
  }

  public void w(String tag, String msg) {
    log(LogLevel.WARN, tag, msg);
  }

  public void e(String tag, String msg) {
    log(LogLevel.ERROR, tag, msg);
  }
}
