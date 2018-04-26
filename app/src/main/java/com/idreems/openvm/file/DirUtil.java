package com.idreems.openvm.file;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.StatFs;
import android.text.TextUtils;

import java.io.File;
import java.io.IOException;

/**
 * Created by ramonqlee on 6/21/16.
 */
public class DirUtil {
    private static final long MIN_SDCARD_SIZE = 10 * 1024 * 1024;  //10M
    private static Context mContext;

    public static void init(Context context) {
        mContext = context;
    }

    public static File getCacheFile(String imageUri) {
        String fileName = FileNameGenerator.urlToFileName(imageUri);
        File file = new File(getAppCacheDir(), fileName);
        return file;
    }


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

    public static String getExternalStorageDirectory() {
        return Environment.getExternalStorageDirectory().getPath();
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
            appCacheDir = new File(new File(dataDir, mContext.getPackageName()), "cache");
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
            String cacheDirPath = "/data/data/" + mContext.getPackageName() + "/cache/";
            appCacheDir = new File(cacheDirPath);
        }
        return appCacheDir;
    }


    private static final String EXTERNAL_STORAGE_PERMISSION = "android.permission.WRITE_EXTERNAL_STORAGE";

    private static boolean hasExternalStoragePermission() {
        if (null == mContext) {
            return false;
        }
        int perm = mContext.checkCallingOrSelfPermission(EXTERNAL_STORAGE_PERMISSION);
        return perm == PackageManager.PERMISSION_GRANTED;
    }

    public static void makesureDirExist(String dir) {
        if (TextUtils.isEmpty(dir)) {
            return;
        }

        File file = new File(dir);
        if (!file.exists()) {
            file.mkdirs();
        }
    }
}