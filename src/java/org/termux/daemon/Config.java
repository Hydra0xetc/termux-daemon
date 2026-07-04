package org.termux.daemon;

import static org.termux.daemon.Logger.LogLevel.*;
import static org.termux.daemon.Logger.LogLevel;

public class Config {
  public static String PROGRAM_NAME = "termux-daemon";
  public static String VERSION = "v1.0.3";

  public static int PORT = 6969;
  public static LogLevel LOG_LEVEL = ERROR;
}
