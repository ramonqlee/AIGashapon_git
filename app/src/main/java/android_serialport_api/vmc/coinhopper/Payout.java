package android_serialport_api.vmc.coinhopper;

import android.content.Context;
import android.text.TextUtils;

import com.idreems.openvm.persistence.Config;

/**
 * Created by ramonqlee on 7/4/16.
 */
public class Payout extends CHProtocolBase {
    public static final byte RESET_PAYOUT_SERIAL_POINTER = 0;   //重置payout序列号
    public static final byte MIN_PAYOUT_SERIAL = 1;     //重置payout序列号
    public static final byte MAX_PAYOUT_SERIAL = 100;   //最大的SN号(预留一些空间)
    private short mPayoutCount;

    // 获取一个可用的SN(会在本地Config中做持久化保存)
    public static byte getSN(Context context) {
        byte r = MIN_PAYOUT_SERIAL;
        try {
            // 首先从本地获取sn，如果从来没设置过，则从0开始
            String val = Config.sharedInstance(context).getValue(Config.COIN_HOPPER_SN);
            if (!TextUtils.isEmpty(val) && TextUtils.isDigitsOnly(val)) {
                r = CHUtils.LOBYTE(Short.valueOf(val));
            }
            r++;

            if (r < MIN_PAYOUT_SERIAL || r > MAX_PAYOUT_SERIAL)
            {
                r = MIN_PAYOUT_SERIAL;
            }
            Config.sharedInstance(context).saveValue(Config.COIN_HOPPER_SN, String.valueOf(r));
        }catch (Exception ex)
        {
            ex.printStackTrace();
        }
        return r;
    }

    // 是否重置sn
    public static boolean isResetSN(byte sn) {
        return RESET_PAYOUT_SERIAL_POINTER == sn;
    }

    public void setPayoutCount(short count) {
        mPayoutCount = count;
    }

    public byte[] getBytes() {
        byte[] ret = new byte[8];
        int c = 0;
        ret[c++] = header();
        ret[c++] = messageLength();
        ret[c++] = getPayoutSN();
        ret[c++] = commandCode();
        ret[c++] = getCOMAddress();
        ret[c++] = CHUtils.LOBYTE(mPayoutCount);
        ret[c++] = CHUtils.HIBYTE(mPayoutCount);
        ret[c++] = CHUtils.xor(ret, ret.length - 1);
        return ret;
    }

    protected byte header() {
        return (byte) 0xED;
    }

    protected byte messageLength() {
        return (byte) 0x08;
    }

    protected byte commandCode() {
        return (byte) 0x50;
    }
}
