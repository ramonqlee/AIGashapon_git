package com.idreems.openvm.file;

import android.content.Context;
import android.text.TextUtils;

import com.idreems.openvm.utils.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ramonqlee on 6/13/16.
 */
public class FileUtil {

    public static void writeFile(String fileName, String write_str, boolean append) {
        try {
            FileOutputStream fout = new FileOutputStream(fileName, append);
            byte[] bytes = write_str.getBytes();

            fout.write(bytes);
            fout.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static byte[] readFile(String fileName) {
        try {
            FileInputStream fin = new FileInputStream(fileName);

            int length = fin.available();

            byte[] buffer = new byte[length];
            fin.read(buffer);
            fin.close();
            return buffer;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean deleteFile(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            return file.delete();
        }
        return true;
    }

    public static void makesureDirExist(String dir) {
        if (TextUtils.isEmpty(dir)) {
            return;
        }

        File file = new File(dir);
        file.mkdirs();
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

    public static void getDirFiles(List<String> fileList, String path, boolean withPath) {
        File[] allFiles = new File(path).listFiles();
        if (null == allFiles) {
            return;
        }
        for (int i = 0; i < allFiles.length; i++) {
            File file = allFiles[i];
            if (file.isFile()) {
                fileList.add(withPath ? (path + File.separator + file.getName()) : file.getName());
            } else if (file.isDirectory()) {
                getDirFiles(fileList, file.getAbsolutePath(), withPath);
            }
        }
    }

    private static final long EXPIRED_TIME = 3 * 30 * 24 * 60 * 60 * 1000;//3个月

    // 进行应用级别的数据清理(清理EXPIRED_TIME个月以上的数据)
    public static void startClean(final Context context) {
        if (null == context) {
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                // 清理临时文件夹
                final long currentTimeInMills = System.currentTimeMillis();
                String tempDir = Utils.getTempCacheDir(context);
                List<String> fileList = new ArrayList<String>();
                Utils.getDirFiles(fileList, tempDir);
                for (String fileName : fileList) {
                    File file = new File(String.format("%s%s%s", tempDir, File.separator, fileName));
                    if (!file.exists()) {
                        continue;
                    }
                    if (currentTimeInMills - file.lastModified() > EXPIRED_TIME) {
                        file.delete();
                    }
                }

                // 清理缓存目录
                String cacheDir = Utils.getCacheDir(context);
                fileList.clear();
                Utils.getDirFiles(fileList, cacheDir);
                for (String fileName : fileList) {
                    File file = new File(String.format("%s%s%s", cacheDir, File.separator, fileName));
                    if (!file.exists()) {
                        continue;
                    }
                    if (currentTimeInMills - file.lastModified() > EXPIRED_TIME) {
                        file.delete();
                    }
                }
            }
        }).start();
    }

    public static boolean isFileExist(String fileFullName) {
        return new File(fileFullName).exists();
    }

    public static boolean makeSureFileExist(String fileFullName) {
        try {
            return new File(fileFullName).createNewFile();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }
}
