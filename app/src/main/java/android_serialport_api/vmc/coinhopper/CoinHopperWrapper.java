package android_serialport_api.vmc.coinhopper;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.idreems.openvm.persistence.Config;
import com.idreems.openvm.utils.LogUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ramonqlee on 7/10/16.
 * 所有处理，包括回调的业务处理，暂时在其中进行
 */
public class CoinHopperWrapper {
    private static final String TAG = "CoinHopper";

    private static CoinHopperWrapper sCoinHopper = new CoinHopperWrapper();
    private static CHAPI sCHAPI;
    private Context mContext;
    private List<Ack.AckCallback> mCallbackList = new ArrayList<Ack.AckCallback>();

    //查询状态通知回调
    private Ack.AckCallback mInquireListener = new Ack.AckCallback() {
        @Override
        public void onStatus(byte sn, byte status, byte address, byte lowDispense, byte highDespense) {
            notifyCallbacks(sn, status, address, lowDispense, highDespense);

            LogUtil.d(TAG, TAG + " inquire result,status =" + status + " sn= " + (short) sn + " reqCount=" + sCoinHopper.getCount() + " lowDispense=" + lowDispense + " highDispense=" + highDespense);

            // 忙的话，直接返回；如果出错或者成功了，则取消定时查询，结束本次出币操作
            if (Status.isBusy(status)) {
                Log.d(TAG, TAG + " payout busy ,status = " + status);
                return;
            }

            if (Status.failed(status)) {
                // TODO 出错处理
                Log.d(TAG, TAG + " payout error,status = " + status);
            }

            if (Status.isSuccessful(status)) {
                // TODO 成功处理
                Log.d(TAG, TAG + " payout successful,status = " + status);
            }

            Log.d(TAG, TAG + " payout complete");
        }
    };

    public static CoinHopperWrapper sharedInstance(final Context context) {
        sCoinHopper.initCoinHopper(context);
        return sCoinHopper;
    }

    /**
     * 初始化，在使用前，必须调用此api进行初始化
     *
     * @param context
     * @return
     */
    private boolean initCoinHopper(Context context) {
        return initCoinHopper(context, false);
    }

    /**
     * 设置异步回调
     *
     * @param callback
     */
    public synchronized void addCallback(Ack.AckCallback callback) {
        if (null == callback || -1 != mCallbackList.indexOf(callback)) {
            return;
        }
        mCallbackList.add(callback);
    }

    public synchronized void removeCallback(Ack.AckCallback callback) {
        if (null == callback) {
            return;
        }
        mCallbackList.remove(callback);
    }

    public synchronized void clearCallback() {
        mCallbackList = new ArrayList<>();
    }

    public synchronized void notifyCallbacks(byte sn, byte status, byte address, byte lowDispense, byte highDespense) {
        if (null == mCallbackList || mCallbackList.isEmpty()) {
            return;
        }

        try {
            Object[] tempList = mCallbackList.toArray();
            for (int i = 0; i < tempList.length; i++) {
                Ack.AckCallback next = (Ack.AckCallback) tempList[i];
                if (null != next) {
                    next.onStatus(sn, status, address, lowDispense, highDespense);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public int getCount() {
        return (null == sCHAPI) ? 0 : sCHAPI.getCount();
    }

    /**
     * 出币，异步回调
     *
     * @param count
     * @return 返回当前出币的序列号
     */
    public byte payoutAsync(int count) {
        if (null == mContext) {
            return Payout.RESET_PAYOUT_SERIAL_POINTER;
        }

        try {
            byte payoutSN = Payout.getSN(mContext);

            // 设置回调监听
            sCHAPI.setCallback(mInquireListener);
            sCHAPI.payout(count, payoutSN);
            return payoutSN;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return Payout.RESET_PAYOUT_SERIAL_POINTER;
    }

    public byte getLastestStatus() {
        return (null == sCHAPI) ? 0 : sCHAPI.getLatestSatus();
    }

    /**
     * 重置机器状态
     * 开机或者机器故障了，调用
     */
    public void resetAsync() {
        if (null == mContext) {
            return;
        }

        try {
            // 设置回调监听
            sCHAPI.setCallback(mInquireListener);
            sCHAPI.reset();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * 查询兑币器的状态
     * 异步返回
     */
    public void inquireStateAsync() {
        if (null == sCHAPI) {
            return;
        }
        sCHAPI.setCallback(mInquireListener);
        sCHAPI.inquireState();
    }

    /**
     * 是否正在出币
     *
     * @return
     */
    public boolean isPayouting() {
        if (null == sCHAPI) {
            return false;
        }
        return sCHAPI.isPayouting();
    }


    private boolean initCoinHopper(Context context, boolean toastLogEnable) {
        if (null == context) {
            return false;
        }
        mContext = context;
        Config config = Config.sharedInstance(context);
        String devicePath = config.getValue(Config.PC_DEVICE);
        String baudRateStr = config.getValue(Config.PC_BAUDE);
        if (null == sCHAPI) {
            sCHAPI = CHAPI.sharedInstance(context);
            if (TextUtils.isEmpty(devicePath) || TextUtils.isEmpty(baudRateStr)) {
                if (toastLogEnable)
                    return false;
            }
        }

        int baudRate = Integer.decode(baudRateStr);
        String comAddStr = config.getValue(Config.COIN_HOPPER_COM);
        byte comAdd = Byte.valueOf(comAddStr);
        sCHAPI.init(devicePath, baudRate, comAdd);
        return true;
    }
}
