package com.idreems.openvm.utils;

import android.content.Context;
import android.text.TextUtils;

import com.idreems.openvm.MyApplication;
import com.idreems.openvm.network.DownloadListener;
import com.idreems.openvm.network.NetworkUtils;
import com.idreems.openvm.persistence.Config;
import com.idreems.openvm.protocols.JsonUtils;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by ramonqlee on 7/29/16.
 */
public class UpdateUtil {
    private static final String TAG = "UpdateUtil";

    public interface VersionCheckListener {
        /**
         * 版本检查失败时的回调
         *
         * @param e
         */
        void onFailure(IOException e);

        /**
         * 检查版本完毕后的回调，返回值控制是否下载升级文件
         *
         * @param serverVersion
         * @return
         */
        boolean onComplete(String serverVersion);
    }

    /**
     * 检查新版本
     */
    public static void startVersionChecker(final Context context,
                                           final String url,
                                           final VersionCheckListener checkListener,
                                           final DownloadListener downloadListener) {
        if (TextUtils.isEmpty(url) || null == context) {
            return;
        }
        LogUtil.d("url = " + url);

        final Callback callback = new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (null != checkListener) {
                    checkListener.onFailure(e);
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (null == checkListener) {
                    return;
                }
                if (null == response) {
                    return;
                }

                String body = response.body().string();
                if (TextUtils.isEmpty(body)) {
                    return;
                }
                String jsZipUrl = "";
                String serverVersion = "";
                try {
                    JSONObject jsonObject = new JSONObject(body);
                    serverVersion = JsonUtils.getString(jsonObject, "version");
                    jsZipUrl = JsonUtils.getString(jsonObject, "url");
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return;
                }
                if (!checkListener.onComplete(serverVersion)) {
                    return;
                }
                Config config = Config.sharedInstance(MyApplication.getContext());
                String node_id = config.getValue(Config.NODE_ID);
                if (jsZipUrl.contains("?")) {
                    jsZipUrl += "&";
                } else {
                    jsZipUrl += "?";
                }
                jsZipUrl += String.format("node_id=%s", node_id);
                FileDownloadUtil.startFileDownload(context, jsZipUrl, downloadListener);
            }
        };
        NetworkUtils.getAndResponseOnThread(url, callback);
    }


}
