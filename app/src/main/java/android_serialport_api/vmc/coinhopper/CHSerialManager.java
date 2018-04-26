package android_serialport_api.vmc.coinhopper;

import android.content.Context;
import android.text.TextUtils;

import com.idreems.openvm.persistence.Config;

import java.util.ArrayList;
import java.util.List;

import android_serialport_api.serialcomm.SerialComm;
import android_serialport_api.vmc.RS232;

/**
 * Created by ramonqlee on 7/7/16.
 */
public class CHSerialManager {
    private static CHSerialManager sCHSerialManager;
    private CHManager mCHManager;
    private List<Ack.AckCallback> mListeners = new ArrayList<Ack.AckCallback>();

    // 创建共享的串口通讯，coin hopper专用对象
    public static CHSerialManager sharedInstance(String devicePath, int baudRate) {
        if (TextUtils.isEmpty(devicePath) || baudRate <= 0) {
            return null;
        }

        if (null != sCHSerialManager) {
            return sCHSerialManager;
        }

        // 配置串口参数，并启动串口通讯
        try {
            SerialComm serialComm = SerialComm.sharedInstace();
            serialComm.config(devicePath, baudRate);
            if(!serialComm.start())
            {
                return null;
            }

            sCHSerialManager = new CHSerialManager();
            sCHSerialManager.init(serialComm);
            return sCHSerialManager;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private void init(SerialComm serialComm) {
        if (null == serialComm) {
            return;
        }
        mCHManager = CHManager.sharedInstance(serialComm);
    }

    //出币流程：包含了对机器状态的重置
    public String send(final Context context,final CHProtocolBase cpProtocolImp) {
        if (null == mCHManager || null == context) {
            return "";
        }

        if (null == cpProtocolImp) {
            return "";
        }

        String reason = Config.sharedInstance(context).getValue(Config.COIN_HOPPER_OFF_REASON);
        if (!TextUtils.isEmpty(reason)) {
            return reason;
        }

        sendCmd(cpProtocolImp, new Ack.AckCallback() {
            @Override
            public void onStatus(byte sn, byte status, byte address,byte lowDispense,byte highDespense) {
                // 本次正常结束了，删除相应的资源,并重置sn号
                mCHManager.removeListener(this);

                // 如果是重置sn的话，则设置到合理的出币范围内
                if (Payout.isResetSN(sn)) {
                    Config.sharedInstance(context).saveValue(Config.COIN_HOPPER_SN, String.valueOf(Payout.MIN_PAYOUT_SERIAL));
                }
                notifyListeners(sn, status, address,lowDispense,highDespense);
            }
        });
        return "";
    }

    public void addListener(Ack.AckCallback l) {
        if (null == l) {
            return;
        }
        if (-1 != mListeners.indexOf(l)) {
            return;
        }
        mListeners.add(l);
    }

    public void removeListener(Ack.AckCallback l) {
        if (null == l) {
            return;
        }
        mListeners.remove(l);
    }

    private void notifyListeners(byte sn, byte status, byte address,byte lowDispense,byte highDespense)
    {
        if (null == mListeners)
        {
            return;
        }

        for (int i = mListeners.size()-1; i >=0; i--)
        {
            Ack.AckCallback callback = mListeners.get(i);
            if (null == callback)
            {
                continue;
            }
            callback.onStatus(sn,status,address,lowDispense,highDespense);
        }
    }

    private void sendCmd(RS232 payout, Ack.AckCallback l) {
        if (null == payout) {
            return;
        }
        if (null != l) {
            mCHManager.addListener(l);
        }

        mCHManager.sendAsync(payout.getBytes());
    }
}
