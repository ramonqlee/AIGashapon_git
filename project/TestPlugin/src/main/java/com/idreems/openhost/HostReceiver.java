package com.idreems.openhost;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Toast;

import com.idreems.openhost.utils.PMHelper;
import com.idreems.openhost.utils.Utils;
import com.morgoo.droidplugin.pm.PluginManager;

// 负责加载第三方app到系统中，启动的事情，app自己负责
public class HostReceiver extends BroadcastReceiver {
    //	public static final String OPENHOST_ACTION = "com.idreems.openhost.action.APK_CHANGED";
    // 指令
    public static final String CMD_KEY = "cmd_key";
    public static final int INVALID_CMD = 0;
    public static final int INSTALL_APK = 0x100;
    public static final int UNINSTALL_APK = INSTALL_APK + 1;
    public static final int UPGRADE_APK = UNINSTALL_APK + 1;
//	public static final int LAUNCH_APK = UPGRADE_APK + 1;// 暂时不需要

    // 参数
    public static final String FILE_NAME = "file_name";    //  安装和升级时，需要传递该参数
//	public static final String PACKAGE_NAME = "package_name";// 卸载时需要传递该参数

    @Override
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
