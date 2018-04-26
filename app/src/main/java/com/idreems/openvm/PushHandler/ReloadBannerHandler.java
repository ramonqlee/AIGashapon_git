package com.idreems.openvm.PushHandler;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.ImageView;

import com.idreems.openvm.Push.PushObserver;
import com.idreems.openvm.R;
import com.idreems.openvm.constant.Consts;
import com.idreems.openvm.file.FileUtil;
import com.idreems.openvm.file.ZipUtil;
import com.idreems.openvm.network.DownloadListener;
import com.idreems.openvm.persistence.Config;
import com.idreems.openvm.protocols.JsonUtils;
import com.idreems.openvm.utils.FileDownloadUtil;
import com.idreems.openvm.utils.LogUtil;
import com.idreems.openvm.utils.ToastUtils;
import com.idreems.openvm.utils.Utils;
import com.idreems.openvm.widget.Task;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static com.idreems.openvm.protocols.JsonUtils.getString;

/**
 * Created by ramonqlee on 8/7/16.
 */
public class ReloadBannerHandler implements PushObserver {
    private static final String TAG = ReloadBannerHandler.class.getSimpleName();

    private static final String HANDLER_TYPE = "UPGRADE_SCREEN";

    private Context mContext;
    private ImageView mBanner;
    private String mTokenStr;
    private long mServerVersion;

    private static Handler sMainHandler = new Handler(Looper.getMainLooper());
    private static final long DISPLAY_PERIOD = 10 * 1000;
    private static int mCurrentPos;//当前显示的图片位置
    private static Timer mTimer;

    public ReloadBannerHandler(Context context) {
        mContext = context;
    }

    public ReloadBannerHandler(Context context, ImageView banner) {
        mContext = context;
        mBanner = banner;
    }

    public void setBanner(ImageView banner) {
        mBanner = banner;
    }

    public boolean onMessage(String message) {
        if (TextUtils.isEmpty(message)) {
            return false;
        }
        try {
            JSONObject jsonObject = new JSONObject(message);
            String type = getString(jsonObject, Task.TASK_KEY);
            if (TextUtils.isEmpty(type) || !TextUtils.equals(type, HANDLER_TYPE)) {
                return false;
            }

            LogUtil.d(Consts.TASK_TAG, "ReloadBannerHandler");
            mTokenStr = Task.getToken();
            // 尝试获取token
            if (TextUtils.isEmpty(mTokenStr)) {
                mTokenStr = getString(jsonObject, Task.TOKEN_KEY);
            }

            Config config = Config.sharedInstance(mContext);
            jsonObject = JsonUtils.getJsonObject(jsonObject, "data");
            mServerVersion = JsonUtils.getLong(jsonObject, "new_version");

            // 本地是否有文件
            final String imagesDir = Utils.getImagesDir(mContext);
            List<String> filesList = new ArrayList<>();
            FileUtil.getDirFiles(filesList, imagesDir);
            if (filesList.isEmpty()) {
                config.saveValue(Config.SCREEN_ZIP_VERSION_ID, "");
            }

            String localZipVersion = config.getValue(Config.SCREEN_ZIP_VERSION_ID);
            LogUtil.d(Consts.TASK_TAG, "ReloadBannerHandler localZipVersion = " + localZipVersion + " mServerVersion=" + mServerVersion);
            if (!TextUtils.isEmpty(localZipVersion)) {
                if (mServerVersion <= Integer.valueOf(localZipVersion)) {
                    return true;
                }
            }

            ToastUtils.show(R.string.new_screen_version_found);
            String downloadUrl = getString(jsonObject, "download_url");
            String node_id = config.getValue(Config.NODE_ID);
            if (downloadUrl.contains("?")) {
                downloadUrl += "&";
            } else {
                downloadUrl += "?";
            }

            downloadUrl += String.format("node_id=%s&token=%s&new_version=%s", node_id, mTokenStr, mServerVersion);
            FileDownloadUtil.startFileDownload(mContext, downloadUrl, filedownLoaderListener);
            LogUtil.d(Consts.TASK_TAG, "ReloadBannerHandler url = " + downloadUrl);
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

    final DownloadListener filedownLoaderListener = new DownloadListener() {
        public void completed(final String filePath) {
            final Runnable threadRunnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        //update version
                        Config config = Config.sharedInstance(mContext);
                        config.saveValue(Config.SCREEN_ZIP_VERSION_ID, String.valueOf(mServerVersion));

                        final String imagesDir = Utils.getImagesDir(mContext);
                        FileUtil.makesureDirExist(imagesDir);

                        // TODO 先暂定显示，然后更新文件后，再重新开始显示
                        stopDisplayImagesOnMainThread(mBanner);

                        List<String> fileList = new ArrayList<>();
                        FileUtil.getDirFiles(fileList, imagesDir, true);
                        //clean first
                        for (String fileName : fileList) {
                            FileUtil.deleteFile(fileName);
                        }

                        //  解压缩到当前目录下
                        LogUtil.d(Consts.TASK_TAG, "ReloadBannerHandler unzip file = " + filePath + " to dir " + imagesDir);
                        ZipUtil.unzip(filePath, imagesDir);
                        FileUtil.deleteFile(filePath);
                        startLoopDisplayImagesOnMainThread(imagesDir, mBanner);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            };
            new Thread(threadRunnable).start();
        }
    };

    public static void startLoopDisplayImagesOnMainThread(String imagesDir, ImageView banner) {
        if (null == banner) {
            return;
        }
        List<String> fileList = new ArrayList<>();
        FileUtil.getDirFiles(fileList, imagesDir, true);
        for (int i = 0; i < fileList.size(); ++i) {
            String fileName = fileList.get(i);
//            fileList.set(i, "file://" + fileName);
            fileList.set(i, fileName);
            LogUtil.d(Consts.TASK_TAG, "ReloadBannerHandler set file = " + fileList.get(i));
        }
        if (fileList.isEmpty()) {
            return;
        }

        startDisplayImagesOnMainThread(fileList, banner);
    }

    public static void stopDisplayImagesOnMainThread(final ImageView banner) {
        if (null == banner) {
            return;
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            stopDisplayImages(banner);
        } else {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    stopDisplayImages(banner);
                }
            });
        }
    }


    public static void startDisplayImagesOnMainThread(final List<String> imageUrls, final ImageView banner) {
        if (null == banner) {
            return;
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            startDisplayImages(imageUrls, banner);
        } else {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    startDisplayImages(imageUrls, banner);
                }
            });
        }
    }

    private static void startDisplayImages(final List<String> imageUrls, final ImageView banner) {
        if (null == imageUrls || imageUrls.isEmpty() || null == banner) {
            return;
        }

        if (null != mTimer) {
            mTimer.cancel();
        }
        mTimer = new Timer();
        final TimerTask task = new TimerTask() {
            @Override
            public void run() {
                final BitmapDrawable bd = Utils.getFullScreenResourceBitmapWithoutCache(banner.getContext(), imageUrls.get(mCurrentPos), 1);
                sMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        banner.setImageDrawable(bd);
                        LogUtil.d(Consts.LOG_TAG, "display pos =" + mCurrentPos + " image = " + imageUrls.get(mCurrentPos));

                        mCurrentPos = (mCurrentPos + 1) % imageUrls.size();
                    }
                });
            }
        };
        mTimer.schedule(task, DISPLAY_PERIOD, DISPLAY_PERIOD);
    }

    private static void stopDisplayImages(final ImageView banner) {
        if (null == mTimer) {
            return;
        }
        mTimer.cancel();
        mTimer = null;
    }
}
