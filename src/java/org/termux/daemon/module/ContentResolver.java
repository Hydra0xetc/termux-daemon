package org.termux.daemon.module;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Locale;

import org.termux.daemon.Logger;

public class ContentResolver {
  private static String TAG = "ContentResolver";

  private static Logger logger = Logger.getInstance();
  private static Object sAmService;
  private static Method sStartActivityMethod;
  private static Class<?>[] sParamTypes;
  private static boolean sPrepared = false;

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

    Class<?> iamInterface =
      Class.forName("android.app.IActivityManager");

    Method best = null;
    int bestCount = -1;

    for (Method m : iamInterface.getMethods()) {
      int count = m.getParameterTypes().length;

      if (m.getName().equals("startActivity")
          && count > bestCount) {
        best = m;
        bestCount = count;
      }
    }

    if (best == null) {
      throw new NoSuchMethodException(
        "startActivity not found"
      );
    }

    best.setAccessible(true);

    sStartActivityMethod = best;
    sParamTypes = best.getParameterTypes();

    sPrepared = true;
  }

  public static  void url(String url) {
    logger.d(TAG, String.format("url(%s)", url));

    try {
      prepare();
    } catch (Exception e) {
      e.printStackTrace();
    }

    try {

      Intent intent = new Intent(Intent.ACTION_VIEW);

      intent.setData(Uri.parse(url));
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

      Object[] callArgs = buildArgs(intent, null);

      sStartActivityMethod.invoke(
        sAmService,
        callArgs
      );

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void file(String path, String mime) {
    try {
      prepare();
    } catch (Exception e) {
      e.printStackTrace();
    }

    String resolvedMime = mime;

    if (resolvedMime == null || resolvedMime.isEmpty()) {
      resolvedMime = guessMimeType(path);
    }

    try {
      Intent intent = new Intent(Intent.ACTION_VIEW);

      Uri uri = Uri.parse(
        "content://com.termux.files" + path
      );

      intent.setDataAndType(uri, resolvedMime);
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

      Object[] callArgs = buildArgs(intent, resolvedMime);

      sStartActivityMethod.invoke(
        sAmService,
        callArgs
      );

    } catch (Throwable t) {
      t.printStackTrace();
    }
  }

  private static Object[] buildArgs(Intent intent, String mime) {
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
          args[i] = mime;
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

  private static String guessMimeType(String path) {
    String ext = "";
    int dot = path.lastIndexOf('.');
    if (dot >= 0) {
      ext = path.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
    String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
    return mime != null ? mime : "*/*";
  }
}
