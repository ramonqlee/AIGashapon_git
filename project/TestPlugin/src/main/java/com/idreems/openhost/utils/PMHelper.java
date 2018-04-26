package com.idreems.openhost.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.RemoteException;
import android.text.TextUtils;

import com.morgoo.droidplugin.pm.PluginManager;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;

public class PMHelper {
	public static void launchPackage(final Context activity,
			final String packageName) {
		if (null == activity || TextUtils.isEmpty(packageName)) {
			return;
		}
		// 先检查下是否已经加载成功了
		if (!isInstalledPackage(packageName)) {
			return;
		}
		launchPackageNoDetect(activity, packageName);
	}

	public static void launchPackageNoDetect(final Context activity,
			final String packageName) {
		try {
			PackageManager pm = activity.getPackageManager();
			final Intent intent = pm.getLaunchIntentForPackage(packageName);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			activity.startActivity(intent);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static boolean isInstalledPackage(String packageName) {
		if (TextUtils.isEmpty(packageName)) {
			return false;
		}
		try {
			return PluginManager.getInstance().getPackageInfo(packageName, 0) != null;
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public static boolean runRootInstall(String apkFileName)
	{
		return runRootCommand(String.format("pm install -r %s", apkFileName));
	}
	/**
	 * 请求ROOT权限后执行命令（最好开启一个线程）
	 * @param cmd	(pm install -r *.apk)
	 * @return
	 */
	public static boolean runRootCommand(String cmd) { 
        Process process = null; 
        DataOutputStream os = null; 
		BufferedReader br = null;
		StringBuilder sb = null;
            try { 
            process = Runtime.getRuntime().exec("su"); 
            os = new DataOutputStream(process.getOutputStream()); 
            os.writeBytes(cmd+"\n"); 
            os.writeBytes("exit\n"); 
			br = new BufferedReader(new InputStreamReader(process.getInputStream()));
			
			sb = new StringBuilder();
			String temp=null;
			while((temp = br.readLine())!=null){
				sb.append(temp+"\n");
				if("Success".equalsIgnoreCase(temp)){
//					LogUtils.logE("----------"+sb.toString());
					return true; 
				}
			}
            process.waitFor(); 
            } catch (Exception e) { 
//                    LogUtils.logE("异常："+e.getMessage()); 
            } 
            finally { 
                try { 
                    if (os != null) { 
                    	os.flush(); 
                        os.close(); 
                    } 
                    if(br!=null){
                    	br.close();
                    }
                    process.destroy(); 
                } catch (Exception e) { 
                	return false; 
                } 
            } 
            return false; 
    }
}
