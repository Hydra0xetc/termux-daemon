package org.termux.daemon;

import android.content.ClipData;

import android.os.ServiceManager;

import java.lang.reflect.Method;

public final class ClipboardModule {
  private static FakeContext fakeCtx;
  private static Class<?> stubClass;
  private static Method asInterface;
  private static Object clipboard;
  private static boolean isPrepared = false;

  private static synchronized void prepare() {
    if (isPrepared) {
      return;
    }

    try {
      fakeCtx = new FakeContext(
        FakeContext.getSystemContext()
      );

      stubClass =
        Class.forName("android.content.IClipboard$Stub");

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
        ? text.toString() + "\n"
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

      ClipData clip =
        ClipData.newPlainText("label", text);

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
