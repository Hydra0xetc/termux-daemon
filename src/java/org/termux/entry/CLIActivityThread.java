package org.termux.entry;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

class LaunchRecord {
  final CLIActivity activity;

  LaunchRecord(CLIActivity activity) {
    this.activity = activity;
  }
}

public final class CLIActivityThread {

  private static final int LAUNCH_ACTIVITY = 1;
  private static final int DESTROY_ACTIVITY = 2;

  private CLIActivity mCurrentActivity;
  private Looper mLooper;
  private Handler mHandler;
  private String[] mArgs;

  public void run(CLIActivity activity, String[] args) {
    mLooper = Looper.myLooper();
    mArgs = args;

    if (mLooper == null) {
      throw new IllegalStateException("Looper not prepared");
    }

    mHandler = new Handler(mLooper) {
      @Override
      public void handleMessage(Message msg) {
        switch (msg.what) {
          case LAUNCH_ACTIVITY:
            launch((LaunchRecord) msg.obj);
            break;

          case DESTROY_ACTIVITY:
            destroy((CLIActivity) msg.obj);
            break;
        }
      }
    };

    scheduleLaunch(activity);

    Looper.loop();
  }

  public void scheduleLaunch(CLIActivity activity) {
    mHandler.obtainMessage(
        LAUNCH_ACTIVITY,
        new LaunchRecord(activity)).sendToTarget();
  }

  public void scheduleDestroy(CLIActivity activity) {
    mHandler.obtainMessage(
        DESTROY_ACTIVITY,
        activity).sendToTarget();
  }

  private void launch(LaunchRecord record) {
    mCurrentActivity = record.activity;

    mCurrentActivity.performCreate((Bundle) null);
    mCurrentActivity.performParseArgs(mArgs);
    mCurrentActivity.performStart();
    mCurrentActivity.performResume();
  }

  private void destroy(CLIActivity activity) {
    activity.performPause();
    activity.performStop();
    activity.performDestroy();

    if (mCurrentActivity == activity) {
      mCurrentActivity = null;
    }

    if (mLooper != null) {
      mLooper.quitSafely();
    }
  }

  public void shutdown() {
    if (mCurrentActivity != null) {
      destroy(mCurrentActivity);
    }
  }

  public void finishCurrentActivity() {
    if (mCurrentActivity != null) {
      scheduleDestroy(mCurrentActivity);
    }
  }

  public Handler getHandler() {
    return mHandler;
  }

  public Looper getLooper() {
    return mLooper;
  }
}
