package org.termux.daemon.service;

import android.content.Intent;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Locale;

import org.termux.daemon.Logger;
import org.termux.daemon.ActivityUtils;

public class ContentResolver {
  private static String TAG = "ContentResolver";

  private static Logger logger = Logger.getInstance();

  public static  void url(String url) {
    logger.d(TAG, String.format("url(%s)", url));
    try {

      Intent intent = new Intent(Intent.ACTION_VIEW);

      intent.setData(Uri.parse(url));
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

      new ActivityUtils(intent).startActivity();

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void file(String path, String mime) {
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

      new ActivityUtils(intent, mime).startActivity();

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
