package org.termux.entry;

import android.content.Context;
import android.os.Looper;

import org.termux.util.FakeContext;

public class Cli {

  public static void main(String[] args) {
    if (Looper.myLooper() == null) {
      Looper.prepare();
    }

    try {

      Context system = FakeContext.getSystemContext();
      FakeContext context = new FakeContext(system);

      CLIActivity activity =
        // for now lets just hardcoded
        // TODO: make a implementation to start
        // not a single activity but can run another CLIActivity
        //
        // NOTE: i think this is can be implementation of CLIstartActivity
        (CLIActivity) Class.forName("org.termux.daemon.MainActivity")
        .getDeclaredConstructor()
        .newInstance();

      CLIActivityThread activityThread = new CLIActivityThread();
      activity.attachThread(activityThread);
      activityThread.run(activity, args);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
