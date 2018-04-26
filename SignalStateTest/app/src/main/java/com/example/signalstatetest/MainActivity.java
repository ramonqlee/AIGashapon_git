
package com.example.signalstatetest;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends Activity {

    private boolean isTop = true;
    private int mPhoneSignalIconId;
    private int[] iconList;
    private int iconLevel;
    
    private ImageView mSignalImageView;
    
    private TextView mTextView;

    /** @hide */
    public static final int SIGNAL_STRENGTH_NONE_OR_UNKNOWN = 0;
    /** @hide */
    public static final int SIGNAL_STRENGTH_POOR = 1;
    /** @hide */
    public static final int SIGNAL_STRENGTH_MODERATE = 2;
    /** @hide */
    public static final int SIGNAL_STRENGTH_GOOD = 3;
    /** @hide */
    public static final int SIGNAL_STRENGTH_GREAT = 4;
    /** @hide */
    public static final int NUM_SIGNAL_STRENGTH_BINS = 5;
    /** @hide */
    public static final String[] SIGNAL_STRENGTH_NAMES = {
        "none", "poor", "moderate", "good", "great"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        mSignalImageView = (ImageView) findViewById(R.id.imageView1);
        mTextView = (TextView) findViewById(R.id.textView2);
        
        if(isTop){
            iconList = TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH[1];
        } else {
            iconList = TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH[0];
        }
        
        mPhoneSignalIconId = R.drawable.stat_sys_signal_null;
        
        
        iconLevel = getIconLevel(12);
        
        mPhoneSignalIconId = iconList[iconLevel];
        
        mSignalImageView.setBackgroundResource(mPhoneSignalIconId);
        
        mTextView.setText(SIGNAL_STRENGTH_NAMES[iconLevel]);
    }

    private int getIconLevel(int num){
            int level;

            // ASU ranges from 0 to 31 - TS 27.007 Sec 8.5
            // asu = 0 (-113dB or less) is very weak
            // signal, its better to show 0 bars to the user in such cases.
            // asu = 99 is a special case, where the signal strength is unknown.
            
//            int asu = getGsmSignalStrength();
            int asu = num;
            if (asu <= 2 || asu == 99) level = SIGNAL_STRENGTH_NONE_OR_UNKNOWN;
            else if (asu >= 12) level = SIGNAL_STRENGTH_GREAT;
            else if (asu >= 8)  level = SIGNAL_STRENGTH_GOOD;
            else if (asu >= 5)  level = SIGNAL_STRENGTH_MODERATE;
            else level = SIGNAL_STRENGTH_POOR;
//            if (DBG) log("getGsmLevel=" + level);
            return level;
        
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch(keyCode){
            case KeyEvent.KEYCODE_DPAD_UP:
                
                iconLevel++;
                
                mPhoneSignalIconId = iconList[iconLevel];
                
                mSignalImageView.setBackgroundResource(mPhoneSignalIconId);
                
                mTextView.setText(SIGNAL_STRENGTH_NAMES[iconLevel]);
                
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                
                iconLevel--;
                
                mPhoneSignalIconId = iconList[iconLevel];
                
                mSignalImageView.setBackgroundResource(mPhoneSignalIconId);
                
                mTextView.setText(SIGNAL_STRENGTH_NAMES[iconLevel]);
                
                break;
        }
        
        
        
        return super.onKeyDown(keyCode, event);
    }

}
