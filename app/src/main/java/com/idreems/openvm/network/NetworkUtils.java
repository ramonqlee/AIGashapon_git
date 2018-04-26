package com.idreems.openvm.network;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.idreems.openvm.MyApplication;
import com.idreems.openvm.utils.DeviceUtils;
import com.idreems.openvm.utils.LogUtil;
import com.idreems.openvm.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by ramonqlee on 4/26/16.
 */
public class NetworkUtils {
    private final static OkHttpClient sOkHttpClient = new OkHttpClient();
    private final static Handler sUIHandler = new Handler(Looper.getMainLooper());

    public static void getAndResponseOnMainThread(final String url, final HttpCallback callback) {
        getAndResponseOnThread(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (null == callback) {
                    return;
                }
                final IOException finalEx = e;
                sUIHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onFailure(finalEx);
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) {
                if (null == callback) {
                    return;
                }
                try {
                    final String responseStr = response.body().string();
                    if (!TextUtils.isEmpty(responseStr)) {
                        LogUtil.d("responseLength = " + responseStr.length());
                    }
                    sUIHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                callback.onResponse(responseStr);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                                LogUtil.d(ex.getMessage());
                            }
                        }
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    public static void getAndResponseOnThread(String url, final Callback callback) {
        Request request = new Request.Builder()
                .url(url)
                .build();

        sOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (null != callback) {
                    callback.onFailure(call, e);
                    ;
                }
            }

            @Override
            public void onResponse(Call call, Response response) {
                try {
                    if (null != callback) {
                        callback.onResponse(call, response);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (null != response && null != response.body()) {
                        response.body().close();
                    }
                }
            }
        });
    }

    // 上传单个文件接口
    public static void postFile(String filePath, MediaType mediaType, String url, final Callback callback) {
        if (TextUtils.isEmpty(filePath) || null == mediaType || TextUtils.isEmpty(url)) {
            return;
        }

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", filePath, RequestBody.create(mediaType, new File(filePath)))
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        sOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (null != callback) {
                    callback.onFailure(call, e);
                    ;
                }
            }

            @Override
            public void onResponse(Call call, Response response) {
                try {
                    if (null != callback) {
                        callback.onResponse(call, response);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (null != response && null != response.body()) {
                        response.body().close();
                    }
                }
            }
        });
    }

    // 通过百度测试网络连通性
    public static void testNetworkConnection(final Callback callback) {
        final String baiduUrl = "http://www.baidu.com";
        NetworkUtils.getAndResponseOnThread(baiduUrl, callback);
    }

    public static boolean isConnected(Context context) {
        if (null == context) {
            return false;
        }
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    public static String getSignedUrl(String url, Map<String, String> parameters) {
        return getSignedUrl(url, parameters, true);
    }

    public static String getSignedUrl(String url, Map<String, String> parameters, boolean withMac) {
        if (TextUtils.isEmpty(url) || null == parameters || 0 == parameters.size()) {
            return url;
        }

        // 增加协议版本信息
        parameters.put("version", "2");
        parameters.put("model", DeviceUtils.model());
        parameters.put("sdk", DeviceUtils.osSDK());

        if (withMac) {
            parameters.put("mac", DeviceUtils.getPreferedMac());
        }

        // 增加当前app的版本信息
        PackageInfo packageInfo = Utils.getPackageInfo(MyApplication.getContext(), MyApplication.getContext().getPackageName());
        if (null != packageInfo) {
            parameters.put("versionName", packageInfo.versionName);
            parameters.put("versionCode", String.valueOf(packageInfo.versionCode));
        }
        StringBuilder r = new StringBuilder(url);
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            if (-1 == r.indexOf("?")) {
                r.append('?');
            } else {
                r.append('&');
            }
            r.append(String.format("%s=%s", entry.getKey(), entry.getValue()));
        }
        // TODO 待增加sign

        return r.toString();
    }
}
