package com.idreems.openhost;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.idreems.openhost.utils.DeviceUtils;
import com.idreems.openhost.utils.DirUtil;
import com.idreems.openhost.utils.PMHelper;
import com.idreems.openhost.utils.ToastUtils;
import com.idreems.openhost.utils.Utils;
import com.morgoo.droidplugin.pm.PluginManager;
import com.morgoo.helper.compat.PackageManagerCompat;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

public class HostService extends Service implements ServiceConnection {
    private static final String TAG = HostService.class.getSimpleName();
    private static final long APP_MONITOR_INTERVAL = 20 * 1000;

    private boolean mLoadedMainApkCMDSent = false;//是否发起了加载主应用的命令

    // 服务连接
    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        if (Utils.logEnabled()) {
            Log.d("Test", "loadMainApk");
        }
        loadMainApk();
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (PluginManager.getInstance().isConnected()) {
            loadMainApk();
        } else {
            PluginManager.getInstance().addServiceConnection(this);
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        PluginManager.getInstance().removeServiceConnection(this);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // 加载主应用
    private void loadMainApk() {
        if (mLoadedMainApkCMDSent) {
            return;
        }

        mLoadedMainApkCMDSent = true;

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 开启启动设置
                    String sdCard = DirUtil.getSDPath();
                    String DefaultLauncherDir = sdCard + File.separator + "DefaultLauncher";
                    Utils.copyAssetFileTo(getAssets(), "default_launcher.xml", DefaultLauncherDir);

                    final String commonApkDir = Utils.getApkCacheDir(HostService.this);
                    Utils.makesureDirExist(commonApkDir);
                    final String mainAppPath = String.format("%s%s%s", commonApkDir, File.separator, DirUtil.getMainAppPackageName() + DirUtil.getApkSuffix());
                    final File mainAppFile = new File(mainAppPath);

                    String updateAppPath = "";
                    String[] temps = DirUtil.getUpdateFileName();
                    for (String val : temps) {
                        updateAppPath = String.format("%s%s%s", commonApkDir, File.separator, val + DirUtil.getApkSuffix());
                        if (new File(updateAppPath).exists()) {
                            break;
                        }
                    }
                    final File updateFile = new File(updateAppPath);

                    // 加载五部曲
                    // 0. 文件合法性校验：检查存在文件的versionCode，如果低于当前Host自带的版本的versionCode，则认为是遗留的版本，删除之(相当于恢复初始版本)
                    // 1. 升级文件和本地文件都存在时，加载versionCode高的那个版本
                    // 2.升级文件单独存在
                    // 3.本地主文件单独存在
                    // 4.自带的asset中的文件

                    // 0. 文件合法性校验
                    final int updateFileVersionCode = Utils.getVersionCode(HostService.this, updateAppPath);
                    final int mainAppFileVersionCode = Utils.getVersionCode(HostService.this, mainAppPath);
                    if (Utils.logEnabled()) {
                        ToastUtils.show(getApplicationContext(), "updateVersionCode=" + updateFileVersionCode + " mainAppVersionCode=" + mainAppFileVersionCode);
                    }
                    PackageInfo packageInfo = getPackageManager().getPackageInfo(HostService.this.getPackageName(), 0);//Host的versionCode
                    if (null != packageInfo) {
                        if (updateFileVersionCode > 0 && packageInfo.versionCode > updateFileVersionCode) {
                            updateFile.delete();
                        }
                        if (mainAppFileVersionCode > 0 && packageInfo.versionCode > mainAppFileVersionCode) {
                            mainAppFile.delete();
                        }
                    }


                    // 0 两个文件都存在的情况
                    if (updateFile.exists() && mainAppFile.exists()) {
                        String newerApkFilePath = mainAppPath;
                        if (updateFileVersionCode > mainAppFileVersionCode) {
                            newerApkFilePath = updateAppPath;
                        }

                        if (Utils.logEnabled()) {
                            ToastUtils.show(getApplicationContext(), "加载文件=" + newerApkFilePath);
                        }

                        if (installPackage(HostService.this, newerApkFilePath, DirUtil.getMainAppPackageName())) {
                            if (Utils.logEnabled()) {
                                ToastUtils.show(getApplicationContext(), "升级最新版本完毕，启动应用!");
                            }
                            PMHelper.launchPackage(HostService.this, DirUtil.getMainAppPackageName());
                            mLoadedMainApkCMDSent = false;
                            return;
                        } else {
                            new File(newerApkFilePath).delete();
                        }
                        return;
                    }


                    // 1 升级文件单独存在
                    if (updateFile.exists()) {
                        if (Utils.logEnabled()) {
                            ToastUtils.show(getApplicationContext(), "加载文件=" + updateAppPath);
                        }
                        if (installPackage(HostService.this, updateAppPath, DirUtil.getMainAppPackageName())) {
                            if (Utils.logEnabled()) {
                                ToastUtils.show(getApplicationContext(), "升级完毕，启动应用!");
                            }
                            PMHelper.launchPackage(HostService.this, DirUtil.getMainAppPackageName());
                            mLoadedMainApkCMDSent = false;
                            return;
                        } else {
                            updateFile.delete();
                        }
                        return;
                    }

                    // 2 有本地主应用文件，则尝试加载，如果成功，则返回；如果失败，则删除之，进入下一步
                    if (mainAppFile.exists()) {
                        if (Utils.logEnabled()) {
                            ToastUtils.show(getApplicationContext(), "加载文件=" + mainAppPath);
                        }
                        if (installPackage(HostService.this, mainAppPath, DirUtil.getMainAppPackageName())) {
                            if (Utils.logEnabled()) {
                                ToastUtils.show(getApplicationContext(), "启动应用!");
                            }
                            PMHelper.launchPackage(HostService.this, DirUtil.getMainAppPackageName());
                            mLoadedMainApkCMDSent = false;
                            return;
                        } else {
                            mainAppFile.delete();
                        }
                        return;
                    }

                    // 3. 没有本地主应用文件，则从asset中释放出来
                    Utils.copyAssetFileTo(getAssets(), DirUtil.getMainAppPackageName() + DirUtil.getApkSuffix(), commonApkDir);
                    if (Utils.logEnabled()) {
                        ToastUtils.show(getApplicationContext(), "加载文件=" + mainAppPath);
                    }
                    if (installPackage(HostService.this, mainAppPath, DirUtil.getMainAppPackageName())) {
                        if (Utils.logEnabled()) {
                            ToastUtils.show(getApplicationContext(), "启动包中自带的应用!");
                        }
                        PMHelper.launchPackage(HostService.this, DirUtil.getMainAppPackageName());
                        mLoadedMainApkCMDSent = false;
                        return;
                    } else {
                        // 删除主应用，提示重试
                        mainAppFile.delete();
                        ToastUtils.show(getApplicationContext(), "启动失败,请重试!");
                    }
                } catch (Exception e) {
                    PMHelper.launchPackage(HostService.this, DirUtil.getMainAppPackageName());
                    e.printStackTrace();
                }

                mLoadedMainApkCMDSent = false;
            }
        }).start();

        // 定时监控，如果没有了，则重启机器
        startAppMonitor();
    }

    private void startAppMonitor() {
        stopAppMonitor();
        if (null == mAppMonitorTimer) {
            mAppMonitorTimer = new Timer();
        }

        final TimerTask task = new TimerTask() {
            @Override
            public void run() {
                // 是否已经加载成功
                if (Utils.isAppAvailable(HostService.this,DirUtil.getMainAppPackageName())) {
                    Log.d(TAG, DirUtil.getMainAppPackageName() + " isRunning");
                    return;
                }
                Log.d(TAG, DirUtil.getMainAppPackageName() + " is not Running,reboot now");
                DeviceUtils.reboot(HostService.this);
            }
        };

        mAppMonitorTimer.schedule(task, APP_MONITOR_INTERVAL, APP_MONITOR_INTERVAL);
        Log.d(TAG, "start monitor " + DirUtil.getMainAppPackageName());
    }

    private void stopAppMonitor() {
        if (null == mAppMonitorTimer) {
            return;
        }
        mAppMonitorTimer.cancel();
        mAppMonitorTimer = null;
    }


    private Timer mAppMonitorTimer = new Timer();

    public static boolean installPackage(final Context context, String filePath, String packageName) {
        if (null == context || TextUtils.isEmpty(filePath) || TextUtils.isEmpty(packageName)) {
            return false;
        }

        int versionCode = Utils.getVersionCode(context, filePath);
        if (versionCode < 0) {
            return false;
        }
        // 如果已经加载过，则比对versionCode，如果本地文件更新，则升级；否则直接删除本地文件，因为没有已经加载的
        try {
            PackageInfo mainAppPackageInfo = PluginManager.getInstance().getPackageInfo(packageName, 0);
            int r = PackageManagerCompat.INSTALL_SUCCEEDED;
            if (null != mainAppPackageInfo) {
                // 版本低于当前已经加载的版本
                if (versionCode < mainAppPackageInfo.versionCode) {
                    return false;
                }
                // 同等版本，等同于加载成功
                if (versionCode == mainAppPackageInfo.versionCode) {
                    return true;
                }

                if (Utils.logEnabled()) {
                    ToastUtils.show(context, "升级应用!");
                }
                r = PluginManager.getInstance().installPackage(filePath, PackageManagerCompat.INSTALL_REPLACE_EXISTING);
            } else {
                r = PluginManager.getInstance().installPackage(filePath, 0);
            }
            if (Utils.logEnabled()) {
                ToastUtils.show(context, "加载结果 = " + r);
            }
            if (PackageManagerCompat.INSTALL_SUCCEEDED == r || PackageManagerCompat.INSTALL_FAILED_ALREADY_EXISTS == r) {
                return true;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean isAppInstalled(String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return false;
        }

        try {
            PackageInfo mainAppPackageInfo = PluginManager.getInstance().getPackageInfo(packageName, 0);
            return (null != mainAppPackageInfo);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
