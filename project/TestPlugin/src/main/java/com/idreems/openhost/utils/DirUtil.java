package com.idreems.openhost.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.StatFs;

import java.io.File;
import java.io.IOException;

/**
 * Created by wxliao on 2015/11/9.
 */
public class DirUtil {
    // FIXME 需要修改为plugin的包名
    private static final String APK_SUFFIX = ".apk";
    private static final String MAIN_APP_PACKAGE_NAME = "com.idreems.openvm";
    private static final String UPDATE_FILE_SUFFIX1 = "_born200911022330";//同主文件一致
    private static final String UPDATE_FILE_SUFFIX2 = "_born201609270923";//同主文件一致
    public static Context mContext;

    public static String getApkSuffix() {
        return APK_SUFFIX;
    }

    public static String getMainAppPackageName() {
        return MAIN_APP_PACKAGE_NAME;
    }

    public static String[] getUpdateFileName() {
        String[] r = {MAIN_APP_PACKAGE_NAME + UPDATE_FILE_SUFFIX1, MAIN_APP_PACKAGE_NAME + UPDATE_FILE_SUFFIX2};
        return r;
    }

    public static void init(Context context) {
        mContext = context;
    }

    public static File getCacheFile(String imageUri) {
        String fileName = FileNameGenerator.urlToFileName(imageUri);
        File file = new File(getAppCacheDir(), fileName);
        return file;
    }


    private static final long MIN_SDCARD_SIZE = 10 * 1024 * 1024;  //10M

    public static boolean isSDCardAvailable() {
        String externalStorageState;
        try {
            externalStorageState = Environment.getExternalStorageState();
        } catch (NullPointerException e) { // (sh)it happens (Issue #660)
            externalStorageState = "";
        } catch (IncompatibleClassChangeError e) { // (sh)it happens too (Issue #989)
            externalStorageState = "";
        }
        if (Environment.MEDIA_MOUNTED.equals(externalStorageState) && hasExternalStoragePermission() && getSDCardAvailableStore() > MIN_SDCARD_SIZE) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 获取存储卡的剩余容量，单位为字节
     *
     * @return availableSpare
     */
    private static long getSDCardAvailableStore() {
        String path = Environment.getExternalStorageDirectory().getPath();
        // 取得sdcard文件路径
        StatFs statFs = new StatFs(path);
        // 获取block的SIZE
        long blocSize = statFs.getBlockSize();
        // 可使用的Block的数量
        long availableBlock = statFs.getAvailableBlocks();
        return availableBlock * blocSize;
    }

    /**
     * 获取图片缓存目录
     *
     * @return
     */
    public static File getAppCacheDir() {
        File appCacheDir = null;
        if (isSDCardAvailable()) {
            File dataDir = new File(new File(Environment.getExternalStorageDirectory(), "Android"), "data");
            appCacheDir = new File(new File(dataDir, MAIN_APP_PACKAGE_NAME), "cache");
            if (!appCacheDir.exists()) {
                appCacheDir.mkdirs();
                try {
                    new File(appCacheDir, ".noMedia").createNewFile();
                } catch (IOException e) {
                }
            }
        }
        if (appCacheDir == null) {
            appCacheDir = mContext.getCacheDir();
        }
        if (appCacheDir == null) {
            String cacheDirPath = "/data/data/" + MAIN_APP_PACKAGE_NAME + "/cache/";
            appCacheDir = new File(cacheDirPath);
        }
        return appCacheDir;
    }


    private static final String EXTERNAL_STORAGE_PERMISSION = "android.permission.WRITE_EXTERNAL_STORAGE";

    private static boolean hasExternalStoragePermission() {
        int perm = mContext.checkCallingOrSelfPermission(EXTERNAL_STORAGE_PERMISSION);
        return perm == PackageManager.PERMISSION_GRANTED;
    }


    public static String getSDPath() {
        File sdDir = null;
        boolean sdCardExist = Environment.getExternalStorageState()
                .equals(android.os.Environment.MEDIA_MOUNTED);//判断sd卡是否存在
        if (sdCardExist) {
            sdDir = Environment.getExternalStorageDirectory();//获取跟目录
        }
        return (null == sdDir) ? "" : sdDir.toString();
    }
}
