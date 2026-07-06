package org.termux.daemon;

import android.os.Looper;
import java.util.ArrayList;

class Arg<T> {
  public final String flag;
  public final String description;
  public final T defaultValue;

  public T value;

  public static ArrayList<Arg> allArgs = new ArrayList<>();

  public Arg(
      String flag,
      T defaultValue,
      String description
  ) {
    this.flag = flag;
    this.defaultValue = defaultValue;
    this.value = defaultValue;
    this.description = description;

    allArgs.add(this);
  }

  public void set(T value) {
    this.value = value;
  }

}

public class Main {
  private static void printHelp(String programName,
      ArrayList<Arg> argsParse) {

    System.out.println("Usage: " + programName + " [OPTIONS]");
    for (Arg a : argsParse) {
      System.out.println("    " + a.flag);
      System.out.println("        " + a.description);
    }

  }

  private static String getVer() {
    return System.getenv("PROGRAM_VER") != null
      ? System.getenv("PROGRAM_VER")
      : "Unknow";
  }

  public static void main(String[] args) throws Exception {
    if (Looper.getMainLooper() == null) {
      Looper.prepare();
    }

    Arg<Integer> port
      = new Arg<>("--port", Config.PORT, "listening port");
    Arg<Boolean> help
      = new Arg<>("--help", false, "print this help message");
    Arg<String> version
      = new Arg<>("--version", getVer(), "show version");

    for (int i = 0; i < args.length; i++) {
      if (args[i].equals(help.flag)) {
        printHelp(Config.PROGRAM_NAME, Arg.allArgs);
        System.exit(0);
      }

      if (args[i].equals(port.flag)) {
        if (i + 1 >= args.length) {
          System.out.println("ERROR: cannot get value after the flag");
          System.exit(1);
        }

        port.set(Integer.valueOf(args[i + 1]));
      }

      if (args[i].equals(version.flag)) {
        System.out.println(version.value);
        System.exit(0);
      }

    }

    ApiServer.start(port.value);

  }
}
