package com.idreems.openhost;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootBroadcastReceiver extends BroadcastReceiver {
	static final String action_boot = "android.intent.action.BOOT_COMPLETED";

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO 判断是否启动，如果启动的话，则直接返回(MainActivity设置为了singleTask，否则容易出问题)
		if (intent.getAction().equals(action_boot)) {
//			Intent ootStartIntent = new Intent(context, MainActivity.class);
//			ootStartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//			context.startActivity(ootStartIntent);
		}
	}

}
