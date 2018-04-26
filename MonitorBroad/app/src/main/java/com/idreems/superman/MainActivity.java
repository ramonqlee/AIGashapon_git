package com.idreems.superman;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import java.util.List;

/**
 * Created by ifensi on 2017/5/3.
 */

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 截取action
        Intent intent = getIntent();
        if (null != intent) {
            final String action = intent.getAction();
            if (TextUtils.equals(SuperMan.REBOOT_BROADCAST_ACTION, action)) {
                Toast.makeText(getApplicationContext(), "收到重启请求", Toast.LENGTH_LONG).show();
                DeviceUtils.reboot(getApplicationContext());
            }

            if (TextUtils.equals(SuperMan.INSTALLAPP_BROADCAST_ACTION, action)) {
                installApp(getApplicationContext(), intent.getStringExtra(SuperMan.FILE_PATH), intent.getStringExtra(SuperMan.PACKAGE_NAME));
            }
        }

//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                finish();
//            }
//        }, 300);
    }

    private void installApp(final Context context, final String filePath, final String packageName) {
        // TODO 静默安装，然后启动
        if (null == context || TextUtils.isEmpty(filePath)) {
            return;
        }
        SilentInstall.install(context,filePath);

        // 启动应用
        doStartApplicationWithPackageName(context, packageName);
    }


    public static void doStartApplicationWithPackageName(Context context, String packagename) {
        if (null == context || TextUtils.isEmpty(packagename)) {
            return;
        }
        // 通过包名获取此APP详细信息，包括Activities、services、versioncode、name等等
        PackageInfo packageinfo = null;
        try {
            packageinfo = context.getPackageManager().getPackageInfo(packagename, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (packageinfo == null) {
            return;
        }

        try {
            // 创建一个类别为CATEGORY_LAUNCHER的该包名的Intent
            Intent resolveIntent = new Intent(Intent.ACTION_MAIN);
            resolveIntent.addCategory(Intent.CATEGORY_LAUNCHER);
//            resolveIntent.addCategory(Intent.CATEGORY_HOME);
            resolveIntent.setPackage(packageinfo.packageName);

            // 通过getPackageManager()的queryIntentActivities方法遍历
            List<ResolveInfo> resolveinfoList = context.getPackageManager()
                    .queryIntentActivities(resolveIntent, 0);

            ResolveInfo resolveinfo = resolveinfoList.iterator().next();
            if (resolveinfo != null) {
                // packagename = 参数packname
                String packageName = resolveinfo.activityInfo.packageName;
                // 这个就是我们要找的该APP的LAUNCHER的Activity[组织形式：packagename.mainActivityname]
                String className = resolveinfo.activityInfo.name;
                // LAUNCHER Intent
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);

                // 设置ComponentName参数1:packagename参数2:MainActivity路径
                ComponentName cn = new ComponentName(packageName, className);

                intent.setComponent(cn);
                context.startActivity(intent);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
