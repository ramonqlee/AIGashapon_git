package com.idreems.openvm.activity;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;

import com.idreems.openvm.MyApplication;
import com.idreems.openvm.R;
import com.idreems.openvm.constant.Consts;
import com.idreems.openvm.persistence.Config;
import com.idreems.openvm.utils.LogUtil;

import android_serialport_api.vmc.AIGashaponMachine.utils.AIGashponManager;


public class DemoActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);
        Button quitButton = (Button)findViewById(R.id.quit_button);
        quitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        Button payoutButton = (Button)findViewById(R.id.payout_button);
        payoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                payoutClick();
            }
        });
    }

    public void payoutClick() {
    }

    private synchronized void payout(int count) {
        Config config = Config.sharedInstance(MyApplication.getContext());
        String devicePath = config.getValue(Config.PC_DEVICE);
        String baudRateStr = config.getValue(Config.PC_BAUDE);
        if (TextUtils.isEmpty(devicePath) || TextUtils.isEmpty(baudRateStr) || !TextUtils.isDigitsOnly(baudRateStr)) {
            LogUtil.e(Consts.HANDLER_TAG, "illegal parameter for devicePath=" + devicePath + " baudRate=" + baudRateStr);
            return;
        }
        int baudRate = Integer.decode(baudRateStr);
        final AIGashponManager wrapper = AIGashponManager.sharedInstance(devicePath, baudRate);
        final byte address = 0;
        final byte groupNo = 2;

        final short timeout = 60;
        wrapper.open(address, groupNo, timeout);
    }

}
