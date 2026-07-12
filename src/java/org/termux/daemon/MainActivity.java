package org.termux.daemon;

import android.os.Looper;
import android.os.Bundle;

import java.util.ArrayList;
import java.io.File;

import org.termux.entry.CLIActivity;
import org.termux.util.Logger;

class Arg<T> {
  public final String flag;
  public final String description;
  public final T defaultValue;

  public T value;

  public static ArrayList<Arg> allArgs = new ArrayList<>();

  public Arg(String flag, T defaultValue, String description) {
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

public class MainActivity extends CLIActivity {
  private static final String TAG = "Main";
  private static Logger logger = Logger.getInstance();

  private Arg<Integer> port;
  private Arg<Boolean> help;
  private Arg<String> version;

  private static void printHelp(String programName,
      ArrayList<Arg> argsParse) {

    System.out.println("Usage: " + programName + " [OPTIONS]");
    for (Arg a : argsParse) {
      System.out.println("    " + a.flag);
      System.out.println("        " + a.description);
    }

  }

  private static String getVerAndBuildType() {
    String version = System.getenv("VERSION") != null
      ? System.getenv("VERSION")
      : "Unknow";
    String buildType = System.getenv("BUILD_TYPE") != null
      ? System.getenv("BUILD_TYPE")
      : "Unknow";
    return String.format("%s-%s", version, buildType);
  }

  @Override
  protected void onCreate(Bundle savedInstance) {
    super.onCreate(savedInstance);

    port = new Arg<>("--port", Config.PORT, "listening port");
    help = new Arg<>("--help", false, "print this help message");
    version = new Arg<>("--version", getVerAndBuildType(), "show version");

  }

  @Override
  protected void onParseArgs(String[] args) {

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

    try {
      ApiServer.start(port.value, this);
    } catch (java.net.BindException e1) {
      logger.e(e1.getMessage());
      System.exit(1);
    } catch (Exception e2) {
      logger.e(TAG, "Unexpected error: " + e2.getMessage());
      e2.printStackTrace();
    }

  }
}
