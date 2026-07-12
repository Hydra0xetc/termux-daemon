package org.termux.util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;

import java.io.File;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Field;

public class FakeContext extends ContextWrapper {

    private static final String FAKE_PACKAGE = "com.termux";
    private static final String TAG = "FakeContext";
    private final Context base;
    private static Logger logger = Logger.getInstance();

    public FakeContext() {
      this(getSystemContextUnchecked());
    }

    public FakeContext(Context base) {
      super(base);
      this.base = base;
    }

    private static Context getSystemContextUnchecked() {
      try {
        return getSystemContext();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public String getPackageName() {
        return FAKE_PACKAGE;
    }

    @Override
    public String getOpPackageName() {
        return FAKE_PACKAGE;
    }

    @Override
    public Context getApplicationContext() {
        return this;
    }

    @Override
    public File getFilesDir() {
        return ensureDir("/data/data/" + FAKE_PACKAGE + "/files");
    }

    @Override
    public File getCacheDir() {
        return ensureDir("/data/data/" + FAKE_PACKAGE + "/cache");
    }

    @Override
    public File getExternalCacheDir() {
        return ensureDir("/sdcard/Android/data/" + FAKE_PACKAGE + "/cache");
    }

    @Override
    public File getExternalFilesDir(String type) {
        String path = "/sdcard/Android/data/" + FAKE_PACKAGE + "/files";
        if (type != null) path += "/" + type;
        return ensureDir(path);
    }

    @Override
    public android.content.res.Resources getResources() {
        return base.getResources();
    }

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        return base.getSharedPreferences(name, mode);
    }

    @Override
    public ContentResolver getContentResolver() {
        return base.getContentResolver();
    }

    @Override
    public Object getSystemService(String name) {
        return base.getSystemService(name);
    }

    public static Context getSystemContext() throws Exception {
        Class<?> atClass = Class.forName("android.app.ActivityThread");
        Constructor<?> ctor = atClass.getDeclaredConstructor();
        ctor.setAccessible(true);
        Object at = ctor.newInstance();
        Method getCtx = at.getClass().getDeclaredMethod("getSystemContext");
        getCtx.setAccessible(true);
        Context ctx = (Context) getCtx.invoke(at);

        // force package name to `FAKE_PACKAGE`
        Field f = ctx.getClass().getDeclaredField("mBasePackageName");
        f.setAccessible(true);
        f.set(ctx, FAKE_PACKAGE);

        Field f2 = ctx.getClass().getDeclaredField("mOpPackageName");
        f2.setAccessible(true);
        f2.set(ctx, FAKE_PACKAGE);

        return ctx;

    }

    private static File ensureDir(String path) {
        File dir = new File(path);
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }
}
