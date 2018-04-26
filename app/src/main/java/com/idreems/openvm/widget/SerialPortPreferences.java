/*
 * Copyright 2009 Cedric Priscal
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */

package com.idreems.openvm.widget;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.serialport.SerialPortFinder;
import android.text.TextUtils;

import com.idreems.openvm.MyApplication;
import com.idreems.openvm.R;
import com.idreems.openvm.constant.Consts;
import com.idreems.openvm.persistence.Config;
import com.idreems.openvm.utils.ToastUtils;
import com.idreems.openvm.utils.Utils;


// 配置设置界面
public class SerialPortPreferences extends PreferenceActivity {

    private SerialPortFinder mSerialPortFinder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setDefaultConfig();

        mSerialPortFinder = new SerialPortFinder();

        addPreferencesFromResource(R.xml.serial_port_preferences);
        final Config config = Config.sharedInstance(this);

        //nodeId
        final String NODE_ID = "NODE_ID";
        final EditTextPreference nodeIdPref = (EditTextPreference) findPreference(NODE_ID);
        String temp = config.getValue(Config.NODE_ID);
        nodeIdPref.setSummary(temp);
        nodeIdPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary((String) newValue);
                config.saveValue(Config.NODE_ID, (String) newValue);
                return true;
            }
        });

        //passwrod
        final String PASSWORD = "PASSWORD";
        EditTextPreference pref = (EditTextPreference) findPreference(PASSWORD);
        temp = config.getValue(Config.PASSWORD);
        pref.setSummary(temp);
        pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary((String) newValue);
                config.saveValue(Config.PASSWORD, (String) newValue);
                return true;
            }
        });

        // Devices
        String PC_DEVICE = "DEVICE";    // 修改时，需要同时修改配置的xml布局文件
        final String device = config.getValue(Config.PC_DEVICE);
        final ListPreference devices = (ListPreference) findPreference(PC_DEVICE);
        String[] entries = mSerialPortFinder.getAllDevices();
        String[] entryValues = mSerialPortFinder.getAllDevicesPath();
        if (null != entries && entries.length > 0 && null != entryValues && entryValues.length > 0) {
            devices.setEntries(entries);
            devices.setEntryValues(entryValues);
            if (TextUtils.isEmpty(device)) {
                devices.setValueIndex(0);
            } else {
                devices.setValue(device);
            }
            devices.setSummary(devices.getValue());
            devices.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    preference.setSummary((String) newValue);
                    config.saveValue(Config.PC_DEVICE, (String) newValue);
                    return true;
                }
            });
        } else {
            devices.setOnPreferenceClickListener(null);
        }

        Preference button = (Preference) getPreferenceManager().findPreference("confirm");
        if (button != null) {
            button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0) {
                    final Config config = Config.sharedInstance(MyApplication.getContext());
                    //  TODO 将这些整理到相应的地方
                    String clientId = config.getValue(Config.NODE_ID);
                    String password = config.getValue(Config.PASSWORD);
                    //是否已经配置
                    if (TextUtils.isEmpty(clientId) || TextUtils.isEmpty(password)) {
                        ToastUtils.show(R.string.invalid_config_pc);
                        return false;
                    }

                    // 重置机器
                    MyApplication.getMyApplication().resetCoinHopper();

                    finish();
                    return true;
                }
            });
        }

        //setting
        final String SETTING = "SETTING";
        Preference settingPref = (Preference) findPreference(SETTING);
        settingPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Utils.safeStartSettings(SerialPortPreferences.this);
                return false;
            }
        });

        Preference openBaiduButton = (Preference) getPreferenceManager().findPreference("openbaidu");
        if (openBaiduButton != null) {
            openBaiduButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0) {
                    openBaidu();
                    return true;
                }
            });
        }

        Preference openFileManagerButton = (Preference) getPreferenceManager().findPreference("filemanager");
        if (openFileManagerButton != null) {
            openFileManagerButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0) {
                    final String packageName = "com.cyanogenmod.filemanager";
                    Utils.doStartApplicationWithPackageName(SerialPortPreferences.this, packageName);
                    return true;
                }
            });
        }

        Preference payoutTestButton = (Preference) getPreferenceManager().findPreference("launcher");
        if (payoutTestButton != null) {
            payoutTestButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0) {
//                    final String packageName = "com.example.defaultlauncher";
                    final String packageName =  "com.android.launcher";
                    Utils.launchapp(SerialPortPreferences.this, packageName);
                    return true;
                }
            });
        }

    }

    private void setDefaultConfig() {
        final Config config = Config.sharedInstance(this);
        config.saveValue(Config.PC_BAUDE, Consts.DEFAULT_BAUD_RATE);

        // 设置缺省的设备path
        String devicePath = config.getValue(Config.PC_DEVICE);
        if (TextUtils.isEmpty(devicePath)) {
            config.saveValue(Config.PC_DEVICE, Consts.DEFAULT_COM_DEVICE);
        }
    }


    private void openBaidu() {
        Intent intent = new Intent();
        intent.setAction("android.intent.action.VIEW");
        Uri content_url = Uri.parse("http://www.baidu.com");
        intent.setData(content_url);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
