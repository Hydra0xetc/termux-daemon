package org.termux.entry;

import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import android.os.Handler;
import android.os.IBinder;
import android.os.Bundle;
import android.os.Looper;
import android.os.ServiceManager;

import android.app.Activity;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;

import org.termux.util.FakeContext;

public class CLIActivity extends FakeContext {
  private static Object sAmService;
  private static Method sStartActivityMethod;
  private static Method getDefault;
  private static Method getService;
  private static Method asInterface;
  private static Class<?> iClipboardStub;
  private static Class<?>[] sParamTypes;
  private static boolean sPrepared = false;
  private FakeContext fakeContext;
  private Context context;
  private CLIActivityThread mThread;

  protected void onParseArgs(String[] args) {
  }

  protected void onCreate(Bundle bundleInstance) {
    if (sPrepared) {
      return;
    }

    try {
      context = FakeContext.getSystemContext();
      fakeContext = new FakeContext(context);
    } catch (Exception e) {
      //TODO: handle exception
    }

    try {
      Object iam;

      try {
        getService = Class
          .forName("android.app.ActivityManager")
          .getMethod("getService");

        iam = getService.invoke(null);

      } catch (Throwable t) {
        Class<?> amn =
          Class.forName("android.app.ActivityManagerNative");
        getDefault = amn.getMethod("getDefault");
        iam = getDefault.invoke(null);
      }

      sAmService = iam;

      Class<?> iamInterface = Class.forName("android.app.IActivityManager");

      Method best = null;
      int bestCount = -1;

      for (Method m : iamInterface.getMethods()) {
        int count = m.getParameterTypes().length;

        if (m.getName().equals("startActivity") && count > bestCount) {
          best = m;
          bestCount = count;
        }
      }

      if (best == null) {
        throw new NoSuchMethodException("startActivity not found");
      }

      best.setAccessible(true);

      sStartActivityMethod = best;
      sParamTypes = best.getParameterTypes();

      sPrepared = true;

    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  protected void onStart() { }
  protected void onResume() { }
  protected void onPause() { }
  protected void onStop() { }
  protected void onDestroy() { }

  private Object[] buildArgs(Intent intent) {
    Object[] args = new Object[sParamTypes.length];

    boolean firstStringUsed = false;
    boolean firstIntUsed = false;

    int intentIndex = -1;
    for (int i = 0; i < sParamTypes.length; i++) {
      if (sParamTypes[i] == Intent.class) {
        intentIndex = i;
        break;
      }
    }

    String resolvedMime = intent.resolveTypeIfNeeded(
      fakeContext.getContentResolver()
    );

    for (int i = 0; i < sParamTypes.length; i++) {
      Class<?> t = sParamTypes[i];
      String tn = t.getName();

      if (tn.equals("android.app.IApplicationThread")) {
        args[i] = null;

      } else if (t == String.class) {
        if (!firstStringUsed) {
          args[i] = "com.termux";
          firstStringUsed = true;
        } else if (intentIndex >= 0 && i == intentIndex + 1) {
          args[i] = resolvedMime;
        } else {
          args[i] = null;
        }

      } else if (t == Intent.class) {
        args[i] = intent;

      } else if (t == IBinder.class) {
        args[i] = null;

      } else if (t == int.class || t == Integer.class) {
        if (!firstIntUsed) {
          args[i] = -1;
          firstIntUsed = true;
        } else {
          args[i] = 0;
        }

      } else if (t == Bundle.class) {
        args[i] = null;

      } else if (tn.equals("android.app.ProfilerInfo")) {
        args[i] = null;

      } else if (t == boolean.class || t == Boolean.class) {
        args[i] = false;

      } else {
        args[i] = null;
      }
    }
    return args;
  }

  @Override
  public void startActivity(Intent intent) {
    if (!sPrepared) {
      throw new IllegalStateException("Activity not prepared");
    }

    try {
      Object[] callArgs = buildArgs(intent);
      Object result = sStartActivityMethod.invoke(sAmService, callArgs);

      if (result instanceof Integer) {
        int code = (Integer) result;
        if (code < 0) {
          // TODO: is there a way to change the code to String??
          throw new RuntimeException("startActivity failed, resultCode=" + code);
        }
      }

    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause();
      throw new RuntimeException("startActivity failed via reflection", cause);
    } catch (Exception e) {
      throw new RuntimeException("startActivity failed via reflection", e);
    }
  }

  public void attachThread(CLIActivityThread thread) {
    mThread = thread;
  }

  public void finish() {
    if (mThread != null) {
      mThread.finishCurrentActivity();
    }
  }

  public final void performCreate(Bundle savedInstance) {
    onCreate(savedInstance);
  }

  public void performParseArgs(String[] args) {
    onParseArgs(args);
  }

  public final void performStart() {
    onStart();
  }

  public final void performResume() {
    onResume();
  }

  public final void performPause() {
    onPause();
  }

  public final void performStop() {
    onStop();
  }

  public final void performDestroy() {
    onDestroy();
  }

  public PackageManager getPackageManager() {
    return context.getPackageManager();
  }

}
