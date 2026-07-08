package org.termux.daemon.service;

import org.termux.daemon.FakeContext;
import org.termux.daemon.Logger;

import android.content.ClipData;

import android.os.ServiceManager;
import android.os.Looper;

import java.lang.reflect.Method;

public final class ClipboardManager {
  private static final String TAG = "ClipboardManager";
  private static Logger logger = Logger.getInstance();
  private static FakeContext fakeCtx;
  private static Class<?> stubClass;
  private static Method asInterface;
  private static Object clipboard;
  private static boolean isPrepared = false;

  private static synchronized void prepare() {
    if (Looper.getMainLooper() == null) {
      Looper.prepare();
    }

    if (isPrepared) {
      return;
    }

    try {
      fakeCtx = new FakeContext(FakeContext.getSystemContext());
      stubClass = Class.forName("android.content.IClipboard$Stub");
      asInterface =
        stubClass.getMethod("asInterface", android.os.IBinder.class);

      clipboard = asInterface.invoke(
        null,
        ServiceManager.getService("clipboard")
      );

      isPrepared = true;

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static String get() {
    try {

      prepare();

      Method getPrimaryClip = clipboard.getClass().getMethod(
        "getPrimaryClip",
        String.class,
        int.class
      );

      ClipData data = (ClipData) getPrimaryClip.invoke(
        clipboard,
        fakeCtx.getPackageName(),
        0
      );

      if (data == null || data.getItemCount() == 0) {
        return "";
      }

      CharSequence text =
        data.getItemAt(0).getText();

      return text != null
        ? text.toString()
        : "";

    } catch (Exception e) {
      e.printStackTrace();
      return "";
    }

  }

  public static void set(String text) {
    try {
      prepare();

      Method setPrimaryClip = clipboard.getClass().getMethod(
        "setPrimaryClip",
        ClipData.class,
        String.class,
        int.class
      );

      ClipData clip = ClipData.newPlainText("label", text);

      setPrimaryClip.invoke(
        clipboard,
        clip,
        fakeCtx.getPackageName(),
        0
      );

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
