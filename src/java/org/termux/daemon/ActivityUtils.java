package org.termux.daemon;

import android.os.IBinder;
import android.os.Bundle;
import android.content.Intent;

import java.lang.reflect.Method;

public class ActivityUtils {
  private Intent intent;
  private String mime;

  private static Object sAmService;
  private static Method sStartActivityMethod;
  private static Class<?>[] sParamTypes;
  private static boolean sPrepared = false;

  public ActivityUtils(Intent intent) {
    this.intent = intent;
  }

  public ActivityUtils(Intent intent, String mime) {
    this.intent = intent;
    this.mime = mime;
  }

  private static synchronized void prepare() throws Exception {
    if (sPrepared) {
      return;
    }

    Object iam;

    try {
      Method getService = Class
        .forName("android.app.ActivityManager")
        .getMethod("getService");

      iam = getService.invoke(null);

    } catch (Throwable t) {
      Class<?> amn =
        Class.forName("android.app.ActivityManagerNative");

      Method getDefault =
        amn.getMethod("getDefault");

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
  }

  private Object[] buildArgs() {
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
          args[i] = this.mime;
        } else {
          args[i] = null;
        }

      } else if (t == Intent.class) {
        args[i] = this.intent;

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

  public void startActivity() {
    try {
      prepare();
      Object[] callArgs = buildArgs();
      sStartActivityMethod.invoke(sAmService, callArgs);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
