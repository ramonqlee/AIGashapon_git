package com.idreems.openvm.PushHandler;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.idreems.openvm.Push.PushObserver;
import com.idreems.openvm.R;
import com.idreems.openvm.constant.Consts;
import com.idreems.openvm.file.FileUtil;
import com.idreems.openvm.network.DownloadListener;
import com.idreems.openvm.persistence.Config;
import com.idreems.openvm.protocols.JsonUtils;
import com.idreems.openvm.utils.DeviceUtils;
import com.idreems.openvm.utils.FileDownloadUtil;
import com.idreems.openvm.utils.LogUtil;
import com.idreems.openvm.utils.ToastUtils;
import com.idreems.openvm.utils.Utils;
import com.idreems.openvm.widget.Task;

import org.json.JSONObject;

import java.io.File;

import static com.idreems.openvm.protocols.JsonUtils.getString;

/**
 * Created by ramonqlee on 8/7/16.
 */
public class UpdateAPPHandler implements PushObserver {
    private static final String TAG = UpdateAPPHandler.class.getSimpleName();

    private static final String HANDLER_TYPE = "UPGRADE_APP";

    // 需要和HostReceiver同步
    public static final String HOST_ACTION = "com.idreems.openvmhost.aciton.APK_CHANGED";
    // 指令
    public static final String CMD_KEY = "cmd_key";
    public static final int INVALID_CMD = 0;
    public static final int INSTALL_APK = 0x100;
    public static final int UNINSTALL_APK = INSTALL_APK + 1;
    public static final int UPGRADE_APK = UNINSTALL_APK + 1;
//	public static final int LAUNCH_APK = UPGRADE_APK + 1;// 暂时不需要

    // 参数
    public static final String FILE_NAME = "file_name";    //  安装和升级时，需要传递该参数

    private boolean mUpdatedApkDownloaded;
    private Context mContext;
    private String mTokenStr;
    private long mServerVersionLong;

    public UpdateAPPHandler(Context context) {
        mContext = context;
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

            mTokenStr = Task.getToken();
            // 尝试获取token
            if (TextUtils.isEmpty(mTokenStr)) {
                mTokenStr = JsonUtils.getString(jsonObject, Task.TOKEN_KEY);
            }

            jsonObject = JsonUtils.getJsonObject(jsonObject, "data");
            mServerVersionLong = JsonUtils.getLong(jsonObject, "new_version");
            PackageInfo packageInfo = Utils.getPackageInfo(mContext, mContext.getPackageName());
            LogUtil.d(Consts.TASK_TAG, "UpdateAPPHandler new_version=" + mServerVersionLong + " packageInfo.versionCode=" + packageInfo.versionCode);
            if (mServerVersionLong <= packageInfo.versionCode) {
                return false;
            }

            // 如果本地已经下载了，不再重复下载，防止出现一直下载的情况
            if (mUpdatedApkDownloaded) {
                //直接发送升级命令
                sendUpdateBroadcast(mContext);
                return true;
            }

            // 如果上次已经下载过了，则无需下载
            String downloadAppVersion = Config.sharedInstance(mContext).getValue(Config.DOWNLOADED_APP_VERSION);
            if (!TextUtils.isEmpty(downloadAppVersion)) {
                if (Long.parseLong(downloadAppVersion) > packageInfo.versionCode) {
                    return true;
                }
            }

            ToastUtils.show(R.string.new_app_version_found);

            String downloadUrl = JsonUtils.getString(jsonObject, "download_url");
            Config config = Config.sharedInstance(mContext);
            String node_id = config.getValue(Config.NODE_ID);
            if (downloadUrl.contains("?")) {
                downloadUrl += "&";
            } else {
                downloadUrl += "?";
            }

            downloadUrl += String.format("node_id=%s&token=%s&new_version=%s", node_id, mTokenStr, mServerVersionLong);
            FileDownloadUtil.startFileDownload(mContext, downloadUrl, filedownLoaderListener);
            LogUtil.d(Consts.TASK_TAG, "UpdateAPPHandler url= " + downloadUrl);
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

    final DownloadListener filedownLoaderListener = new DownloadListener() {
        public void completed(final String filePath) {
            final String downloadedApkFilePath = filePath;
            final Runnable threadRunnable = new Runnable() {
                @Override
                public void run() {
                    final String finalApkCacheDir = Utils.getApkCacheDir(mContext);
                    // 解压到最终的目录下，这样Host就不用再解压了
                    FileUtil.makesureDirExist(finalApkCacheDir);
                    final String mainAppFileName = mContext.getPackageName() + Utils.getApkSuffix();
                    final String updateApkFileName = Utils.getUpdateApkFileName(mContext);

                    // 检查升级文件和当前主文件apk文件哪个闲置，将其作为待升级的文件:原则和当前加载应用的versionCode不一致
                    //缺省为主应用的文件名
                    // 情况1. 包名命名，被加载；下载了新的apk，此时应该用升级文件名
                    // 情况2：升级文件名，被加载；下载了新的apk，此时应该用包名命名
                    String localUpdateFileName = mainAppFileName;
                    final String updateFilePath = String.format("%s%s%s", finalApkCacheDir, File.separator, updateApkFileName);
                    int updateFileVersionCode = Utils.getVersionCode(mContext, updateFilePath);
                    PackageInfo packageInfo = Utils.getPackageInfo(mContext, mContext.getPackageName());

                    // 主文件
                    if (null != packageInfo) {
                        if (updateFileVersionCode != packageInfo.versionCode) {
                            localUpdateFileName = updateApkFileName;
                        }
                    }

                    if (Utils.extractFromApkAssets(downloadedApkFilePath, finalApkCacheDir, mContext.getPackageName() + Utils.getApkSuffix(), localUpdateFileName)) {
                        mUpdatedApkDownloaded = true;//设置已经下载的标志
                        FileUtil.deleteFile(downloadedApkFilePath);

                        // 保存当前下载的app版本
                        Config.sharedInstance(mContext).saveValue(Config.DOWNLOADED_APP_VERSION, String.valueOf(mServerVersionLong));
                        // 清理文件
                        sendUpdateBroadcast(mContext);
                    }
                }
            };
            new Thread(threadRunnable).start();
        }
    };

    private void sendUpdateBroadcast(final Context context) {
        // FIXME 当前是否空闲

        ToastUtils.show(R.string.update_app_tip);
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                // 直接重启
                DeviceUtils.reboot(context);
            }
        }, 4000);
    }
}
