package android_serialport_api.vmc.coinhopper;

import android_serialport_api.vmc.RS232;

/**
 * Created by ramonqlee on 7/5/16.
 */
public abstract class CHProtocolBase implements RS232 {
    private byte mComAdd;
    private byte mPayoutNo;

    abstract public byte[] getBytes();

    abstract protected byte header();

    abstract protected byte messageLength();

    abstract protected byte commandCode();

    // 设置com端口地址
    public void setCOMAddress(byte comAdd) {
        mComAdd = comAdd;
    }

    protected byte getCOMAddress() {
        return mComAdd;
    }

    // 设置出币的序列编号
    public void setPayoutSN(byte payoutNo) {
        mPayoutNo = payoutNo;
    }

    public byte getPayoutSN() {
        return mPayoutNo;
    }

}
