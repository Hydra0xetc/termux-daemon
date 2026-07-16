package org.termux.daemon.service;

import org.termux.util.FakeContext;
import org.termux.util.Logger;

import android.os.ServiceManager;
import android.os.Looper;

import java.lang.reflect.Method;

import android.content.Context;
import android.content.ClipData;
import android.content.ClipboardManager;

public final class ClipboardService {
  private static final String TAG = "ClipboardService";
  private static Logger logger = Logger.getInstance();
  private static ClipboardManager clipboard;

  private static synchronized void prepare(Context context) {
    clipboard = (ClipboardManager) context.getSystemService(context.CLIPBOARD_SERVICE);
  }

  public static String get(Context context) {
    if (clipboard == null) {
      logger.d(TAG, "logger is null prepare it only if ClipboardManager is null");
      prepare(context);
    }

    ClipData clip = clipboard.getPrimaryClip();

    if (clip == null || clip.getItemCount() == 0) {
      return "";
    }

    CharSequence text = clip.getItemAt(0).getText();

    return text != null ? text.toString() : "";

  }

  public static void set(Context context, String text) {
    if (clipboard == null) {
      logger.d(TAG, "logger is null prepare it only if ClipboardManager is null");
      prepare(context);
    }
    ClipData clip = ClipData.newPlainText("label", text);
    clipboard.setPrimaryClip(clip);
  }

}
