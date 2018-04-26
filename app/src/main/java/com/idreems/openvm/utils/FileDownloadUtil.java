package com.idreems.openvm.utils;

import android.content.Context;
import android.util.Log;

import com.idreems.openvm.constant.Consts;
import com.idreems.openvm.file.FileUtil;
import com.idreems.openvm.network.DownloadListener;
import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.FileDownloadListener;
import com.liulishuo.filedownloader.FileDownloader;
import com.liulishuo.filedownloader.model.FileDownloadStatus;

import java.io.File;

/**
 * Created by ramonqlee on 8/6/16.
 */
public class FileDownloadUtil {
    private static final String TAG = "FileDownloadUtil";

    public static synchronized void startFileDownload(Context context, final String url, final DownloadListener downloadListener) {
        final String tempCacheDir = Utils.getTempCacheDir(context);
        final String tempCachedFileName = String.format("%s%s%s", tempCacheDir, File.separator, Utils.getFileNameByUrl(url));
        FileUtil.makesureDirExist(tempCacheDir);

        BaseDownloadTask task = FileDownloader.getImpl().create(url).setPath(tempCachedFileName);
        task.setListener(new FileDownloadListener() {
            private static final long LOG_INTERVAL = 2000;
            private long mProgressLatestLogTime;

            @Override
            protected void pending(BaseDownloadTask task, int soFarBytes, int totalBytes) {
            }

            @Override
            protected void progress(BaseDownloadTask task, int soFarBytes, int totalBytes) {
                if (0 == mProgressLatestLogTime | System.currentTimeMillis() - mProgressLatestLogTime > LOG_INTERVAL) {
                    LogUtil.d(Consts.TASK_TAG, "startFileDownload progress =" + soFarBytes + " totalBytes=" + totalBytes);
                    mProgressLatestLogTime = System.currentTimeMillis();
                }
                if (null == downloadListener) {
                    return;
                }
                downloadListener.progress(soFarBytes, totalBytes);
            }

            @Override
            protected void blockComplete(BaseDownloadTask task) {
            }

            @Override
            protected void completed(BaseDownloadTask task) {
                LogUtil.d(Consts.TASK_TAG, "startFileDownload completed");
                if (null == downloadListener) {
                    return;
                }
                downloadListener.completed(task.getPath());
            }

            @Override
            protected void paused(BaseDownloadTask task, int soFarBytes, int totalBytes) {
            }

            @Override
            protected void error(BaseDownloadTask task, Throwable e) {
                LogUtil.d(Consts.TASK_TAG, "startFileDownload error =" + ((null != e) ? e.getLocalizedMessage() : ""));
                if (null == downloadListener) {
                    return;
                }
                downloadListener.error(e);
            }

            @Override
            protected void warn(BaseDownloadTask task) {
            }
        });

        task.setAutoRetryTimes(10);
        if (FileDownloadStatus.isIng(FileDownloader.getImpl().getStatus(url,tempCachedFileName))) {
            Log.d(TAG, "url's file is downloading " + url);
            return;
        }
        task.start();
    }
}
