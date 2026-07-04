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
      = new ArgsParse<>("--port", 6969, "listening port");
    ArgsParse<Boolean> help
      = new ArgsParse<>("--help", false, "print this help message");
    argsParse.add(port);
    argsParse.add(help);

    for (int i = 0; i < args.length; i++) {
      if (args[i].equals(help.flag)) {
        help.set(true);
      }

      if (args[i].equals(port.flag)) {
        if (i + 1 >= args.length) {
          System.out.println("ERROR: cannot get value after the flag");
          System.exit(1);
        }

        port.set(Integer.valueOf(args[i + 1]));
      }
    }

    ApiServer.start(port.value);

    if (help.value) {
      printHelp(Config.PROGRAM_NAME, argsParse);
    }

  }
}
