package android_serialport_api.vmc.coinhopper;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.idreems.openvm.persistence.Config;

import java.util.Timer;
import java.util.TimerTask;


/**
 * Created by ramonqlee on 7/9/16.
 */
public class CHAPI {
    private static final String TAG = "CHAPI";
    private static final long PAYOUT_TIME_OUT = 2000;//2s
    private static final long INQUIRE_PERIOD = 1000; //查询的间隔

    private static CHAPI sCHAPI;
    private static CHSerialManager sCHSerialManager;

    private byte mPayoutSN;
    private Context mContext;
    private boolean mPayouting;//出币中
    private int mCount;
    private Timer mInquireTimer;
    private String mDevicePath;
    private int mBaudRate;
    private byte mCHComAddress;//coin hopper端口地址
    private byte mLatestSatus = 0;

    private Ack.AckCallback mInquireCallback;//对外的通知

    public static CHAPI sharedInstance(Context context) {
        if (null == sCHAPI) {
            sCHAPI = new CHAPI();
        }
        sCHAPI.mContext = context;
        return sCHAPI;
    }

    public void init(String devicePath, int baudRate, byte chComAddress) {
        mDevicePath = devicePath;
        mBaudRate = baudRate;
        mCHComAddress = chComAddress;
    }

    public void setCallback(Ack.AckCallback callback) {
        mInquireCallback = callback;
    }

    // 查询状态，会在回调中返回
    public void inquireState() {
        // 发送一次查询命令
        final byte sn = 0;// sn在查询时，不重要
        startInquire(getContext(), sn, mInquireListener);
    }

    public boolean isPayouting() {
        return mPayouting;
    }

    public byte getLatestSatus()
    {
        return mLatestSatus;
    }
    public int getCount()
    {
        return mCount;
    }
    public String payout(int count, byte sn) {
        mCount = count;
        try {
            if (TextUtils.isEmpty(mDevicePath)) {
                Log.e(TAG, "init first");
                return "";
            }

            if (null == getContext()) {
                return "";
            }

            Config config = Config.sharedInstance(getContext());
            if (null == sCHSerialManager) {
                sCHSerialManager = CHSerialManager.sharedInstance(mDevicePath, mBaudRate);
            }

            if (mPayouting) {
                if (null != mInquireCallback) {
                    mInquireCallback.onStatus(sn, Ack.STATUS_CODE_BUSY, mCHComAddress,(byte)0,(byte)0);
                }
                return "";
            }

            //出币
            Payout payout = new Payout();
            mPayoutSN = sn;
            // 合法性校验
            if (mPayoutSN > Payout.MAX_PAYOUT_SERIAL || mPayoutSN < Payout.MIN_PAYOUT_SERIAL) {
                mPayoutSN = Payout.MIN_PAYOUT_SERIAL;
            }

            // 更新sn
            config.saveValue(Config.COIN_HOPPER_SN, String.valueOf(mPayoutSN));

            // 设置具体的参数
            payout.setPayoutSN(mPayoutSN);
            payout.setPayoutCount((short) count);
            payout.setCOMAddress(mCHComAddress);

            //同时启动定时查询处理
            startInquireLoop(getContext(), mPayoutSN, mInquireListener, PAYOUT_TIME_OUT, INQUIRE_PERIOD);
            mPayouting = true;
            String reason = sCHSerialManager.send(getContext(), payout);
            Log.d(TAG, "start payouting,sn =" + mPayoutSN + " count= " + count);

            if (!TextUtils.isEmpty(reason)) {
//                Toast.makeText(this, "coin hopper已关闭,reason= " + reason, Toast.LENGTH_LONG).show();
            }
            return reason;
        } catch (Exception ex) {
            mPayouting = false;
            ex.printStackTrace();
        }
        return "";
    }

    private static void startInquire(Context context, final byte sn, Ack.AckCallback l) {
        try {
            if (null == sCHSerialManager || null == context) {
                return;
            }

            String comAddStr = Config.sharedInstance(context).getValue(Config.COIN_HOPPER_COM);
            byte comAdd = Byte.valueOf(comAddStr);

            //查询状态
            Inquire inquire = new Inquire();
            // 设置具体的参数
            inquire.setPayoutSN(sn);
            inquire.setCOMAddress(comAdd);
            sCHSerialManager.addListener(l);
            sCHSerialManager.send(context, inquire);
            Log.d(TAG, "start startInquire,sn=" + sn);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    //查询状态通知回调
    private Ack.AckCallback mInquireListener = new Ack.AckCallback() {
        @Override
        public void onStatus(byte sn, byte status, byte address,byte lowDispense,byte highDispense) {
            if (null == sCHSerialManager) {
                return;
            }
            if (null != mInquireCallback) {
                mInquireCallback.onStatus(sn, status, address,lowDispense,highDispense);
            }
            Log.d(TAG, TAG+" inquire inquire,status =" + status + " sn=" + sn + " lowDispense = "+lowDispense+" highDispense ="+highDispense);

            mLatestSatus = status;
            // 忙的话，直接返回；如果出错或者成功了，则取消定时查询，结束本次出币操作
            if (Status.isBusy(status)) {
                return;
            }
            mPayouting = false;
            //释放资源
            sCHSerialManager.removeListener(mInquireListener);
            if (null != mInquireTimer) {
                mInquireTimer.cancel();
                mInquireTimer = null;
            }
        }
    };

    private void startInquireLoop(final Context context, final byte sn, final Ack.AckCallback l, long delay, long period) {
        if (null != mInquireTimer) {
            mInquireTimer.cancel();
            mInquireTimer = null;
        }

        mInquireTimer = new Timer();
        mInquireTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                startInquire(context, sn, l);
            }
        }, delay, period);
        Log.d(TAG, "startInquireLoop");
    }

    private Context getContext() {
        return mContext;
    }

    // 在配置完毕或者应用启动时调用，用于设备重置
    public void reset() {
        try {
            if (TextUtils.isEmpty(mDevicePath)) {
                Log.e(TAG, "init first");
                return;
            }

            if (null == getContext()) {
                return;
            }

            if (null == sCHSerialManager) {
                sCHSerialManager = CHSerialManager.sharedInstance(mDevicePath, mBaudRate);
            }
            final byte sn = Payout.RESET_PAYOUT_SERIAL_POINTER;

            if (mPayouting) {
                // TODO 回调，状态忙
//                Toast.makeText(this, "出币中", Toast.LENGTH_LONG).show();
                if (null != mInquireCallback) {
                    mInquireCallback.onStatus(sn, Ack.STATUS_CODE_BUSY, mCHComAddress,(byte)0,(byte)0);
                }
                return;
            }

            //出币
            Payout payout = new Payout();
            mPayoutSN = sn;
            mCount = 0;
            // 设置具体的参数
            payout.setPayoutSN(mPayoutSN);
            payout.setCOMAddress(mCHComAddress);

            //同时启动定时查询处理
            startInquireLoop(getContext(), mPayoutSN, mInquireListener, PAYOUT_TIME_OUT, INQUIRE_PERIOD);
            sCHSerialManager.send(getContext(), payout);
            mPayouting = true;
            Log.d(TAG, "start payouting,sn =" + mPayoutSN);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


}
