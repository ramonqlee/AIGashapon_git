package com.idreems.openvm.utils;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.idreems.openvm.MyApplication;
import com.idreems.openvm.constant.Consts;
import com.idreems.openvm.file.FileUtil;
import com.idreems.openvm.file.ZipUtil;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by ramonqlee on 7/28/16.
 */
public class LogUtil {
    public static final String LOG_TAG = Consts.LOG_TAG;
    private static final int OBSOLETE_COUNT_4_LOG = 7;//保留最近N个日志文件，考虑连续和间断的情况，采用个数

    public static void d(String msg) {
        if (TextUtils.isEmpty(msg)) {
            return;
        }
        d(LOG_TAG, msg);
    }

    public static void d(Exception e) {
        if (null == e || TextUtils.isEmpty(e.getMessage())) {
            return;
        }

        d(e.getMessage());
    }

    public static void e(String tag, String msg) {
        try {
            Log.e(tag, msg);

            SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
            String time = format.format(new Date());
            String formatedMsg = String.format(Locale.getDefault(), "%s\t%s\n\n", time, msg);
            log2LocalFile(formatedMsg);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void d(String tag, String msg) {
        try {
            Log.d(tag, msg);

            SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
            String time = format.format(new Date());
            String formatedMsg = String.format(Locale.getDefault(), "%s\t%s\n\n", time, msg);
            log2LocalFile(formatedMsg);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static void writeLocalFile(String filePath, String msg) {
        try {
            int pos = filePath.lastIndexOf(File.separator);
            if (-1 == pos) {
                return;
            }
            String logDir = filePath.substring(0, pos + 1);
            FileUtil.makesureDirExist(logDir);
            // 确保文件存在
            FileUtil.makeSureFileExist(filePath);
            //写入文件
            FileUtil.writeFile(filePath, msg, true);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // 写入到本地文件中
    private static void log2LocalFile(String msg) {
        try {
            //按照天进行归集,文件名采用天进行命名
            String logDir = Utils.getLogCacheDir(MyApplication.getMyApplication());
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            String fileName = format.format(new Date());
            String filePath = String.format("%s%s%s.log", logDir, File.separator, fileName);

            writeLocalFile(filePath, msg);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // 将日志文件打包，返回打包后的路径名
    public static String zipLogFiles(Context context, String fileNamePrefix) {
        if (null == context) {
            return "";
        }
        try {
            String logDir = Utils.getLogCacheDir(MyApplication.getMyApplication());
            File zippedFile = context.getFilesDir().createTempFile(fileNamePrefix, ".zip");
            ZipUtil.zip(logDir, zippedFile.getAbsolutePath());
            return zippedFile.getAbsolutePath();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "";
    }

    // 清理废弃的文件，仅仅保留N天的日志文件
    public static void cleanJunkLogFiles() {
        // TODO 每天清理一次,超过期限的进行清理
        List<String> logFiles = new ArrayList<String>();
        final String logDir = Utils.getLogCacheDir(MyApplication.getMyApplication());
        Utils.getDirFiles(logFiles, logDir);
        if (logFiles.size() < OBSOLETE_COUNT_4_LOG) {
            return;
        }

        //  按照时间升序，对所有的文件进行排序，删除前logFiles.size()-OBSOLETE_COUNT_4_LOG个
        Collections.sort(logFiles, new Comparator<String>() {
            @Override
            public int compare(String lhs, String rhs) {

                File lhsFile = new File(String.format("%s%s%s", logDir, File.separator, lhs));
                File rhsFile = new File(String.format("%s%s%s", logDir, File.separator, rhs));
                if (!lhsFile.exists() || !rhsFile.exists()) {
                    return 0;
                }

                return (int) (lhsFile.lastModified() - rhsFile.lastModified());
            }
        });

        for (int i = 0; i < logFiles.size() - OBSOLETE_COUNT_4_LOG; ++i) {
            FileUtil.deleteFile(String.format("%s%s%s", logDir, File.separator, logFiles.get(i)));
        }
    }

    //将指定byte数组以16进制的形式打印到控制台
    public static String printHexString(String tag, byte[] b) {
        StringBuilder r = new StringBuilder();
        for (int i = 0; i < b.length; i++) {
            String hex = Integer.toHexString(b[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            r.append(hex.toUpperCase() + " ");
        }
        return r.toString();
    }

}
