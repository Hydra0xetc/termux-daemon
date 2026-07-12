package org.termux.daemon.service;

import android.net.Uri;

import android.os.Looper;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.termux.util.Logger;

class ApkEntry {
  public String packageName;
  public String className;

  public ApkEntry(String packageName, String className) {
    this.packageName = packageName;
    this.className = className;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof ApkEntry)) return false;

    ApkEntry other = (ApkEntry) obj;
    return packageName.equals(other.packageName)
      && className.equals(other.className);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(packageName, className);
  }

}

public class ApkManager {
  private static final String TAG = "ApkManager";
  private static HashMap<String, ApkEntry> entryList = new HashMap<>();
  private static Logger logger = Logger.getInstance();

  public static String scanApk(Context context) {
    if (Looper.getMainLooper() == null) {
      Looper.prepare();
    }

    try {
      PackageManager pm = context.getPackageManager();
      Intent intent = new Intent(Intent.ACTION_MAIN)
        .addCategory(Intent.CATEGORY_LAUNCHER);
      List<ResolveInfo> resolverInfos = pm.queryIntentActivities(intent, 0);

      HashMap<String, ApkEntry> newEntryList = new HashMap<>();

      for (ResolveInfo info : resolverInfos) {
        ApkEntry entry = new ApkEntry(
          info.activityInfo.packageName,
          info.activityInfo.name
        );

        newEntryList.put(info.loadLabel(pm).toString(), entry);
      }

      if (!entryList.equals(newEntryList)) {
        entryList = newEntryList;
      }

      return "INFO: Done scanning...";

    } catch (Exception e) {
      e.printStackTrace();
      return "ERROR: failed to scan: " + e.getMessage();
    }

  }

  public static String openApk(Context context, String apkName) {
    if (entryList.isEmpty()) {
      scanApk(context);
    }

    ApkEntry entry = entryList.get(apkName);
    if (entry == null) {
      return "ERROR: could not found apk: " + apkName;
    }

    Intent intent = new Intent()
      .setClassName(entry.packageName, entry.className)
      .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

    context.startActivity(intent);
    return null;
  }

  public static List<String> listApk(Context context) {
    if (entryList.isEmpty()) {
      scanApk(context);
    }

    List<String> apkList = new ArrayList<>(entryList.keySet());
    Collections.sort(apkList);
    return apkList;
  }

  public static String uninstallApk(Context context, String apkName) {
    if (entryList.isEmpty()) {
      scanApk(context);
    }

    ApkEntry entry = entryList.get(apkName);
    if (entry == null) {
      return "ERROR: could not found apk: " + apkName;
    }

    logger.d(TAG, entry.packageName);

    Uri packageUri = Uri.parse("package:" + entry.packageName);
    Intent intent = new Intent(Intent.ACTION_DELETE, packageUri);
    context.startActivity(intent);

    return null;
  }

}
