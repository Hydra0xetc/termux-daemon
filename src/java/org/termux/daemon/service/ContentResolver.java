package org.termux.daemon.service;

import android.content.Intent;
import android.content.Context;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Locale;

import org.termux.util.Logger;

public class ContentResolver {
  private static String TAG = "ContentResolver";

  private static Logger logger = Logger.getInstance();

  public static  void url(Context context, String url) {
    logger.d(TAG, String.format("url(%s)", url));
    try {

      Intent intent = new Intent(Intent.ACTION_VIEW);

      intent.setData(Uri.parse(url));
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

      context.startActivity(intent);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void file(Context context, String path, String mime) {
    String resolvedMime = mime;

    if (resolvedMime == null || resolvedMime.isEmpty()) {
      resolvedMime = guessMimeType(path);
    }

    try {
      Intent intent = new Intent(Intent.ACTION_VIEW);

      Uri uri = Uri.parse("content://com.termux.files" + path);

      intent.setDataAndType(uri, resolvedMime);
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

      context.startActivity(intent);

    } catch (Throwable t) {
      t.printStackTrace();
    }
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
