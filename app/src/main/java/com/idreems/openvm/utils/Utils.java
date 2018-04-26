package com.idreems.openvm.utils;

import android.app.ActivityManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.idreems.openvm.file.DirUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 */
public class Utils {
    private static final String APK_SUFFIX = ".apk";
    private static final String UPDATE_FILE_SUFFIX = "_born200911022330";//"_born201609270923";
    private static final int READ_BUFFER_SIZE = 4096 * 10;//40k

    public static String getApkSuffix() {
        return APK_SUFFIX;
    }

    public static String getUpdateFileSuffix() {
        return UPDATE_FILE_SUFFIX;
    }

    public static String getUpdateApkFileName(Context context) {
        if (null == context) {
            return "";
        }
        return context.getPackageName() + UPDATE_FILE_SUFFIX + APK_SUFFIX;
    }

    public static void safeStartSettings(Context context) {
        try {
            context.startActivity(new Intent(Settings.ACTION_SETTINGS));
        } catch (ActivityNotFoundException ex) {
            try {
                context.startActivity(new Intent(Intent.ACTION_VIEW)
                        .setComponent(new ComponentName("com.android.settings", "com.android.settings.Settings")));
            } catch (ActivityNotFoundException e) {

            }
        }
    }

    public static String md5(String string) {
        byte[] hash;
        try {
            hash = MessageDigest.getInstance("MD5").digest(string.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Huh, MD5 should be supported?", e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Huh, UTF-8 should be supported?", e);
        }

        StringBuilder hex = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            if ((b & 0xFF) < 0x10) hex.append("0");
            hex.append(Integer.toHexString(b & 0xFF));
        }
        return hex.toString();
    }

    public static boolean isFileExist(String fileFullName) {
        return new File(fileFullName).exists();
    }

    public static String getTempCacheDir(Context context) {
        return String.format("%s%s%s", context.getFilesDir(), File.separator, "mytempcache");
    }

    public static String getCacheDir(Context context) {
        return String.format("%s%s%s", context.getFilesDir(), File.separator, "mycache");
    }

    public static String getImagesDir(Context context) {
        return String.format("%s%s%s", context.getFilesDir(), File.separator, "images");
    }

    public static String getDirUnderFileDir(Context context, String subDir) {
        if (null == context || TextUtils.isEmpty(subDir)) {
            return "";
        }
        return String.format("%s%s%s", context.getFilesDir(), File.separator, subDir);
    }

    // 放到一个host和plugin都可以访问的地方
    public static String getApkCacheDir(Context context) {
        return String.format("%s%s%s", DirUtil.getAppCacheDir(), File.separator, "apks");
    }

    public static String getLogCacheDir(Context context) {
        return String.format("%s%s%s", DirUtil.getAppCacheDir(), File.separator, "logs");
    }

    public static String getWebviewCacheDir(Context context) {
        return String.format("%s%s%s", DirUtil.getAppCacheDir(), File.separator, "www");
    }

    public static String getWebviewCandidateCacheDir(Context context) {
        return String.format("%s%s%s", DirUtil.getAppCacheDir(), File.separator, "www2");
    }

    public static String getFileNameByUrl(String url) {
        return Utils.md5(url);
    }

    public static void moveFile(String srcFileName, String destFileName, boolean rewrite) {
        File fromFile = new File(srcFileName);
        File toFile = new File(destFileName);

        if (!fromFile.exists()) {
            return;
        }
        if (!fromFile.isFile()) {
            return;
        }
        if (!fromFile.canRead()) {
            return;
        }
        if (!toFile.getParentFile().exists()) {
            toFile.getParentFile().mkdirs();
        }
        if (toFile.exists() && rewrite) {
            toFile.delete();
        }
        //当文件不存时，canWrite一直返回的都是false
        try {
            java.io.FileInputStream fosfrom = new java.io.FileInputStream(fromFile);
            FileOutputStream fosto = new FileOutputStream(toFile);
            byte bt[] = new byte[1024];
            int c;
            while ((c = fosfrom.read(bt)) > 0) {
                fosto.write(bt, 0, c); //将内容写到新文件当中
            }
            fosfrom.close();
            fosto.close();

            // delete original file
            if (fromFile.exists()) {
                fromFile.delete();
            }
        } catch (Exception ex) {
            Log.e("Test", ex.getMessage());
        }
    }

    public static void getDirFiles(List<String> fileList, String path) {
        File[] allFiles = new File(path).listFiles();
        if (null == allFiles) {
            return;
        }
        for (int i = 0; i < allFiles.length; i++) {
            File file = allFiles[i];
            if (file.isFile()) {
                fileList.add(file.getName());
            } else if (file.isDirectory()) {
                getDirFiles(fileList, file.getAbsolutePath());
            }
        }
    }

    // -1代表参数非法或者文件非法
    public static int getVersionCode(Context context, String apkPath) {
        final int INVALID_VERSION_CODE = -1;
        if (null == context || TextUtils.isEmpty(apkPath) || !new File(apkPath).exists()) {
            return INVALID_VERSION_CODE;
        }
        try {
            final PackageManager pm = context.getPackageManager();
            PackageInfo info = pm.getPackageArchiveInfo(apkPath, 0);
            return null != info ? info.versionCode : INVALID_VERSION_CODE;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return INVALID_VERSION_CODE;
    }

    public static PackageInfo getPackageInfo(Context context, String packageName) {
        if (null == context || TextUtils.isEmpty(packageName)) {
            return null;
        }
        try {
            return context.getPackageManager().getPackageInfo(packageName, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // TODO 从apk的asset目录中提取所需的文件
    public static boolean extractFromApkAssets(final String apkFile, final String destPath, final String filteredFile, String saveAsFileName) {
        File destFile = new File(destPath);
        if (!destFile.exists()) {
            destFile.mkdirs();
        }

        return unzip(new File(apkFile), destFile, filteredFile, saveAsFileName);
    }

    public static boolean unzip(final File file, final File destination, String filteredFileName, String saveAsFileName) {
        try {
            ZipInputStream zin = new ZipInputStream(new FileInputStream(file));
            String workingDir = destination.getAbsolutePath() + "/";

            byte buffer[] = new byte[4096];
            int bytesRead;
            ZipEntry entry = null;
            while ((entry = zin.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    File dir = new File(workingDir, entry.getName());
                    if (!dir.exists()) {
                        dir.mkdir();
                    }
//                            Log.i(LOG_TAG, "[DIR] "+entry.getName());
                } else {
                    if (!TextUtils.isEmpty(filteredFileName) && !TextUtils.isEmpty(entry.getName()) &&
                            !entry.getName().contains(filteredFileName)) {
                        continue;
                    }
                    FileOutputStream fos = new FileOutputStream(workingDir + saveAsFileName);
                    while ((bytesRead = zin.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                    fos.close();
//                            Log.i(LOG_TAG, "[FILE] "+entry.getName());
                }
            }
            zin.close();
            return true;

//                    Log.i(LOG_TAG, "COMPLETED in "+(ELAPSED_TIME/1000)+" seconds.");
        } catch (Exception e) {
//                    Log.e(LOG_TAG, "FAILED");
            e.printStackTrace();
        }
        return false;
    }


    /**
     * 执行拷贝任务
     *
     * @param asset 需要拷贝的assets文件路径
     * @return 拷贝成功后的目标文件句柄
     * @throws IOException
     */
    public static boolean copyAssetFileTo(AssetManager assetManager,
                                          String assetName, String destDir) throws IOException {
        if (null == assetManager || TextUtils.isEmpty(assetName)
                || TextUtils.isEmpty(destDir)) {
            return false;
        }
        InputStream source = assetManager.open(assetName);
        File destinationFile = new File(destDir + File.separator
                + assetName);
        destinationFile.getParentFile().mkdirs();
        OutputStream destination = new FileOutputStream(destinationFile);
        byte[] buffer = new byte[READ_BUFFER_SIZE];
        int nread;

        while ((nread = source.read(buffer)) != -1) {
            if (nread == 0) {
                nread = source.read();
                if (nread < 0)
                    break;
                destination.write(nread);
                continue;
            }
            destination.write(buffer, 0, nread);
        }
        destination.close();
        return true;
    }

    public static String getSha1(String str) {
        if (null == str || 0 == str.length()) {
            return null;
        }
        char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                'a', 'b', 'c', 'd', 'e', 'f'};
        try {
            MessageDigest mdTemp = MessageDigest.getInstance("SHA1");
            mdTemp.update(str.getBytes("UTF-8"));

            byte[] md = mdTemp.digest();
            int j = md.length;
            char[] buf = new char[j * 2];
            int k = 0;
            for (int i = 0; i < j; i++) {
                byte byte0 = md[i];
                buf[k++] = hexDigits[byte0 >>> 4 & 0xf];
                buf[k++] = hexDigits[byte0 & 0xf];
            }
            return new String(buf);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 启动薄荷App
     *
     * @param context
     */
    public static void launchapp(Context context, String packageName) {
        // 判断是否安装过App，否则去市场下载
        try {
            if (isAppInstalled(context, packageName)) {
                Intent intent = context.getPackageManager().getLaunchIntentForPackage(packageName);
                if (null == intent) {
                    intent = new Intent();
                    intent.addCategory(Intent.CATEGORY_HOME);
                    intent.setAction(Intent.ACTION_MAIN);
                }
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } else {
                goToMarket(context, packageName);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * 去市场下载页面
     */
    public static void goToMarket(Context context, String packageName) {
        Uri uri = Uri.parse("market://details?id=" + packageName);
        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
        try {
            context.startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {
        }
    }

    /**
     * 检测某个应用是否安装
     *
     * @param context
     * @param packageName
     * @return
     */
    public static boolean isAppInstalled(Context context, String packageName) {
        try {
            context.getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    public static void doStartApplicationWithPackageName(Context context, String packagename) {
        // 通过包名获取此APP详细信息，包括Activities、services、versioncode、name等等
        PackageInfo packageinfo = null;
        try {
            packageinfo = context.getPackageManager().getPackageInfo(packagename, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (packageinfo == null) {
            return;
        }

        try {
            // 创建一个类别为CATEGORY_LAUNCHER的该包名的Intent
            Intent resolveIntent = new Intent(Intent.ACTION_MAIN);
            resolveIntent.addCategory(Intent.CATEGORY_LAUNCHER);
//            resolveIntent.addCategory(Intent.CATEGORY_HOME);
            resolveIntent.setPackage(packageinfo.packageName);

            // 通过getPackageManager()的queryIntentActivities方法遍历
            List<ResolveInfo> resolveinfoList = context.getPackageManager()
                    .queryIntentActivities(resolveIntent, 0);

            ResolveInfo resolveinfo = resolveinfoList.iterator().next();
            if (resolveinfo != null) {
                // packagename = 参数packname
                String packageName = resolveinfo.activityInfo.packageName;
                // 这个就是我们要找的该APP的LAUNCHER的Activity[组织形式：packagename.mainActivityname]
                String className = resolveinfo.activityInfo.name;
                // LAUNCHER Intent
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);

                // 设置ComponentName参数1:packagename参数2:MainActivity路径
                ComponentName cn = new ComponentName(packageName, className);

                intent.setComponent(cn);
                context.startActivity(intent);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * 判断相对应的APP是否存在
     *
     * @param context
     * @param packageName(包名)(若想判断QQ，则改为com.tencent.mobileqq，若想判断微信，则改为com.tencent.mm)
     * @return
     */
    public static boolean isAppAvailable(Context context, String packageName) {
        PackageManager packageManager = context.getPackageManager();

        //获取手机系统的所有APP包名，然后进行一一比较
        List<PackageInfo> pinfo = packageManager.getInstalledPackages(0);
        for (int i = 0; i < pinfo.size(); i++) {
            if (((PackageInfo) pinfo.get(i)).packageName
                    .equalsIgnoreCase(packageName))
                return true;
        }
        return false;
    }

    public static void makesureDirExist(String dir) {
        if (TextUtils.isEmpty(dir)) {
            return;
        }

        File file = new File(dir);//DirUtil.getCacheFile(bean.appUrl);
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    /**
     * @param context
     * @param resId
     * @param scaleFactor 大于1，将图片缩小
     * @return
     */
    public static BitmapDrawable getFullScreenResourceBitmapWithoutCache(Context context, String resId, int scaleFactor) {
        if (0 == scaleFactor) {
            scaleFactor = 1;
        }
        final int width = DeviceUtils.getScreenMetrics(context).getWidth() / scaleFactor;
        final int height = DeviceUtils.getScreenMetrics(context).getHeight() / scaleFactor;
        return getResourceBitmapDrawableWithoutCache(context, resId, width, height);
    }

    public static BitmapDrawable getResourceBitmapDrawableWithoutCache(Context context, String resId, int width, int height) {
// bugfix 降低图片采样率
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(resId,options);
//        BitmapFactory.decodeResource(context.getResources(), resId, options);
        options.inSampleSize = Utils.computeSampleSize(options, width, height);
        options.inJustDecodeBounds = false;
        options.inPurgeable = true;
        options.inDither = false;
        options.inPreferredConfig = Bitmap.Config.RGB_565;
//        Bitmap bm = BitmapFactory.decodeResource(context.getResources(), resId, options);

        Bitmap bm = BitmapFactory.decodeFile(resId, options);
        return new BitmapDrawable(context.getResources(), bm);
    }

    public static int computeSampleSize(BitmapFactory.Options options,
                                        int reqWidth, int reqHeight) {
        // 源图片的高度和宽度
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            // 计算出实际宽高和目标宽高的比率
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);
            // 选择宽和高中最小的比率作为inSampleSize的值，这样可以保证最终图片的宽和高
            // 一定都会大于等于目标的宽和高。
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        return inSampleSize;
    }

    public static String getProcessName(Context cxt, int pid) {
        ActivityManager am = (ActivityManager) cxt
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningApps = am.getRunningAppProcesses();
        if (runningApps == null) {
            return null;
        }
        for (ActivityManager.RunningAppProcessInfo procInfo : runningApps) {
            if (procInfo.pid == pid) {
                return procInfo.processName;
            }
        }
        return null;
    }

    public static boolean isAppRunnnig(Context context, String processName) {
        // 获取activity管理对象
        ActivityManager activityManager = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        // 获取所有正在运行的app
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager
                .getRunningAppProcesses();
        // 遍历app，获取应用名称或者包名
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (TextUtils.equals(processName, appProcess.processName)) {
                return true;
            }
        }
        return false;
    }
}
