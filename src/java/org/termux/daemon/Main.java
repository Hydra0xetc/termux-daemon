package org.termux.daemon;

import android.os.Looper;
import java.util.ArrayList;

class ArgsParse<T> {
  public final String flag;
  public final String description;
  public final T defaultValue;

  public T value;

  public ArgsParse(
      String flag,
      T defaultValue,
      String description
  ) {
    this.flag = flag;
    this.defaultValue = defaultValue;
    this.value = defaultValue;
    this.description = description;
  }

  public void set(T value) {
    this.value = value;
  }

}

public class Main {
  private static int port = 6969;
  private static ArrayList<ArgsParse> argsParse = new ArrayList<>();

  private static void printHelp(String programName,
      ArrayList<ArgsParse> argsParse) {

    System.out.println("Usage: " + programName + " [OPTIONS]");
    for (ArgsParse f : argsParse) {
      System.out.println("    " + f.flag);
      System.out.println("        " + f.description);
    }

  }

  public static void main(String[] args) throws Exception {
    if (Looper.getMainLooper() == null) {
      Looper.prepare();
    }

    ArgsParse<Integer> port
      = new ArgsParse<>("--port", Config.PORT, "listening port");
    ArgsParse<Boolean> help
      = new ArgsParse<>("--help", false, "print this help message");

    ArgsParse<String> version
      = new ArgsParse<>("--version", Config.VERSION, "show version");
    argsParse.add(port);
    argsParse.add(help);
    argsParse.add(version);

    for (int i = 0; i < args.length; i++) {
      if (args[i].equals(help.flag)) {
        printHelp(Config.PROGRAM_NAME, argsParse);
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
