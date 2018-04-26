package com.idreems.openhost.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Utils {
    private static final int READ_BUFFER_SIZE = 4096 * 10;//40k

    // 升级说明：
    // 1. 关闭调试日志
    // 2. 将主应用放到asset目录下，命名采用其报名，然后将本应用的versionCode修改为同主应用一致
    // FIXME 测试日志开关
    public static boolean logEnabled() {
        return false;
    }

    // 放到一个host和plugin都可以访问的地方
    public static String getApkCacheDir(Context context) {
        return String.format("%s%s%s", DirUtil.getAppCacheDir(), File.separator, "apks");
    }

    public static String getTempApkCacheDir(Context context) {
        return String.format("%s%s%s", context.getFilesDir(), File.separator, "tmpapks");
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
        // if (!toFile.canWrite()) {
        // MessageDialog.openError(new Shell(),"错误信息","不能够写将要复制的目标文件" + toFile.getPath());
        // Toast.makeText(this,"不能够写将要复制的目标文件", Toast.LENGTH_SHORT);
        // return ;
        // }
        try {
            java.io.FileInputStream fosfrom = new java.io.FileInputStream(fromFile);
            java.io.FileOutputStream fosto = new FileOutputStream(toFile);
            byte bt[] = new byte[READ_BUFFER_SIZE];
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


    /**
     * 执行拷贝任务
     *
     * @param asset 需要拷贝的assets文件路径
     * @return 拷贝成功后的目标文件句柄
     * @throws IOException
     */
    public static boolean copyAssetFileTo(final AssetManager assetManager,
                                          final String assetName, final String destDir) throws IOException {
        if (null == assetManager || TextUtils.isEmpty(assetName)
                || TextUtils.isEmpty(destDir)) {
            return false;
        }

        String destDirWithSeparator = destDir;
        // 确认是否有分隔符
        if (!TextUtils.equals(destDir.substring(destDir.length() - 1), File.separator)) {
            destDirWithSeparator = destDir + File.separator;
        }

        InputStream source = assetManager.open(assetName);
        File destinationFile = new File(destDirWithSeparator + assetName);
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

    // TODO 从apk的asset目录中提取所需的文件
    public static boolean extractFromApkAssets(final String apkFile,
                                               final String destPath, final String filteredFile) {
        File destFile = new File(destPath);
        if (!destFile.exists()) {
            destFile.mkdirs();
        }
        return unzip(new File(apkFile), destFile, filteredFile);
    }

    public static boolean unzip(final File file, final File destination,
                                String filteredFileName) {
        boolean r = false;
        try {
            ZipInputStream zin = new ZipInputStream(new FileInputStream(file));
            String workingDir = destination.getAbsolutePath() + File.separator;

            byte buffer[] = new byte[READ_BUFFER_SIZE];
            int bytesRead;
            ZipEntry entry = null;
            while ((entry = zin.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    File dir = new File(workingDir, entry.getName());
                    if (!dir.exists()) {
                        dir.mkdir();
                    }
                } else {
                    if (!TextUtils.isEmpty(filteredFileName)
                            && !TextUtils.isEmpty(entry.getName())
                            && !entry.getName().contains(filteredFileName)) {
                        continue;
                    }
                    FileOutputStream fos = new FileOutputStream(workingDir
                            + filteredFileName);
                    while ((bytesRead = zin.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                    fos.close();
                    r = true;
                }
            }
            zin.close();
        } catch (Exception e) {
            e.printStackTrace();
            r = false;
        }
        return r;
    }

    public static void makesureDirExist(String dir) {
        if (TextUtils.isEmpty(dir)) {
            return;
        }

        File file = new File(dir);// DirUtil.getCacheFile(bean.appUrl);
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    // -1代表参数非法或者文件非法
    public static int getVersionCode(Context context, String apkPath) {
        if (null == context || TextUtils.isEmpty(apkPath) || !new File(apkPath).exists()) {
            return -1;
        }
        try {
            final PackageManager pm = context.getPackageManager();
            PackageInfo info = pm.getPackageArchiveInfo(apkPath, 0);
            return ((null != info) ? info.versionCode : -1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static PackageInfo getPackageInfo(Context context, String apkPath) {
        if (null == context || TextUtils.isEmpty(apkPath) || !new File(apkPath).exists()) {
            return null;
        }
        try {
            final PackageManager pm = context.getPackageManager();
            return pm.getPackageArchiveInfo(apkPath, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
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
}
