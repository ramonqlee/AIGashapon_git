package com.idreems.openvm.protocols.clouds;

import android.content.Context;
import android.text.TextUtils;

import com.idreems.openvm.file.FileUtil;
import com.idreems.openvm.file.ZipUtil;
import com.idreems.openvm.network.NetworkUtils;
import com.idreems.openvm.protocols.JsonUtils;
import com.idreems.openvm.utils.Utils;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Response;

/**
 * Created by ramonqlee on 5/18/16.
 */
public class GetLogHandler extends CloudBaseHandler {
    public static final String MY_TOPIC = "get_log";

    public GetLogHandler(Context context) {
        super(context);
    }

    @Override
    public String name() {
        return MY_TOPIC;
    }

    protected boolean handleContent(JSONObject content) {
        /**
         * “content”: {
         “dir”: “sale”,
         “date”: “2016-05-10”,
         “url”: “http://abc.com/log/upload”,
         “token”: “a2ksk9bsd” //上传日志时，以get参数携带此token
         }
         */
        boolean r = false;
        if (null == content) {
            return r;
        }

        r = true;
        // 选定待上传的文件和上传的方式
        String dir = JsonUtils.getString(content, CloudConsts.DIR);
        String date = JsonUtils.getString(content, CloudConsts.DATE);
        String url = JsonUtils.getString(content, CloudConsts.URL);
        String token = JsonUtils.getString(content, CloudConsts.TOKEN);
        if (TextUtils.isEmpty(dir) || TextUtils.isEmpty(date) || TextUtils.isEmpty(url)) {
            return r;
        }

        StringBuilder postUrl = new StringBuilder(url);
        if (!TextUtils.isEmpty(token)) {
            if (-1 == postUrl.indexOf("?")) {
                postUrl.append('?');
            } else {
                postUrl.append('&');
            }
            postUrl.append(String.format("%s=%s", CloudConsts.TOKEN, token));
        }

        final String logDir = Utils.getDirUnderFileDir(mContext, dir);
        String fileName = String.format("%s%s%s", logDir, File.separator, date);
        postLogFile(fileName, postUrl.toString());

        return r;
    }

    private void postLogFile(final String fileName, final String url) {
        // 上传日志文件
        //1. 压缩为zip
        //2. 上传
        //3. 上传成功，删除本地zip和本地源文件
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String zipFile = String.format("%s.zip", fileName);
                try {
                    ZipUtil.zip(fileName,zipFile);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return;
                }

                MediaType mediaType = MediaType.parse("application/zip; charset=utf-8");
                NetworkUtils.postFile(zipFile, mediaType, url, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (response.isSuccessful()) {
                            FileUtil.deleteFile(zipFile);
                            FileUtil.deleteFile(fileName);
                        }
                    }
                });
            }
        }).start();
    }
}
