package org.termux.util;

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

  // log with tag
  private void logT(LogLevel level, String tag, String msg) {
    String prefix = switch (level) {
      case INFO  -> "[INFO]";
      case DEBUG -> "[DEBUG]";
      case WARN  -> "[WARN]";
      case ERROR -> "[ERROR]";
    };

    System.out.printf("%s [%s] %s\n", prefix, tag, msg);
  }

  // regular log without tag
  private void log(LogLevel level, String msg) {
    String prefix = switch (level) {
      case INFO  -> "[INFO]";
      case DEBUG -> "[DEBUG]";
      case WARN  -> "[WARN]";
      case ERROR -> "[ERROR]";
    };

    System.out.printf("%s %s\n", prefix, msg);
  }

  // log with tag
  public void d(String tag, String msg) {
    logT(LogLevel.DEBUG, tag, msg);
  }

  public void i(String tag, String msg) {
    logT(LogLevel.INFO, tag, msg);
  }

  public void w(String tag, String msg) {
    logT(LogLevel.WARN, tag, msg);
  }

  public void e(String tag, String msg) {
    logT(LogLevel.ERROR, tag, msg);
  }

  // log without tag
  public void d(String msg) {
    log(LogLevel.DEBUG, msg);
  }

  public void i(String msg) {
    log(LogLevel.INFO, msg);
  }

  public void w(String msg) {
    log(LogLevel.WARN, msg);
  }

  public void e(String msg) {
    log(LogLevel.ERROR, msg);
  }
}
