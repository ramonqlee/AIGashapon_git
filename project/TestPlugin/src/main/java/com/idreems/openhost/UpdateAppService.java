package com.idreems.openhost;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.widget.Toast;

import com.idreems.openhost.utils.PMHelper;
import com.idreems.openhost.utils.Utils;
import com.morgoo.droidplugin.pm.PluginManager;

import static com.idreems.openhost.HostReceiver.CMD_KEY;
import static com.idreems.openhost.HostReceiver.FILE_NAME;
import static com.idreems.openhost.HostReceiver.INSTALL_APK;
import static com.idreems.openhost.HostReceiver.INVALID_CMD;
import static com.idreems.openhost.HostReceiver.UNINSTALL_APK;
import static com.idreems.openhost.HostReceiver.UPGRADE_APK;

/**
 * Created by ramonqlee on 06/11/2016.
 */

public class UpdateAppService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onStart(Intent intent, int startId)
    {
        onReceive(this,intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        onReceive(this,intent);
        return START_STICKY;
    }

    public void onReceive(Context context, Intent intent) {
        // 根据收到的数据，做相应的操作
        // 1. 安装新的apk
        // 2. 卸载apk
        // 3. 更新apk
        if (Utils.logEnabled()) {
            Toast.makeText(context, "收到广播：" + intent.getAction(), Toast.LENGTH_LONG)
                    .show();
        }

        final int r = intent.getIntExtra(CMD_KEY, INVALID_CMD);
        if (INVALID_CMD == r) {
            return;
        }

        final String fileName = intent.getStringExtra(FILE_NAME);
        if (TextUtils.isEmpty(fileName)) {
            return;
        }
        String packageName = "";
        if (TextUtils.isEmpty(packageName)) {
            // 从文件中提取出包名
            PackageInfo packageInfo = Utils.getPackageInfo(context, fileName);
            if (null != packageInfo) {
                packageName = packageInfo.packageName;
            }
        }

        if (Utils.logEnabled()) {
            Toast.makeText(context, "收到加载文件请求：" + fileName + " 操作码=" + r, Toast.LENGTH_LONG)
                    .show();
        }

        final Context finalContext = context;
        final String finalPackageName = packageName;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    boolean ret = false;
                    if (INSTALL_APK == r || UPGRADE_APK == r) {
                        ret = HostService.installPackage(finalContext, fileName, finalPackageName);
                    } else if (UNINSTALL_APK == r) {
                        PluginManager.getInstance().deletePackage(finalPackageName, 0);
                    }
//					else if(LAUNCH_APK == r){
//						ret= HostService.installPackage(finalContext, fileName, finalPackageName);
//					}
                    PMHelper.launchPackage(finalContext, finalPackageName);

                    if (Utils.logEnabled()) {
                        final boolean finalRet = ret;
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(finalContext,
                                        "动态加载操作结果 = " + finalRet, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
