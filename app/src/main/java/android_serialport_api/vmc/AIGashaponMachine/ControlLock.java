package android_serialport_api.vmc.AIGashaponMachine;

import android_serialport_api.vmc.RS232;

/**
 * Created by ramonqlee on 10/05/2017.
 */

public abstract class ControlLock extends Location implements RS232, Command, Instruction {
    private byte mGroupNo;//局部地址，取值1-3
    private short mTimeout;

    // 指令内部区分码，用于同一类指令划分为不同的用途
    public abstract byte controlTypeCode();

    @Override
    public byte getCode() {
        return (byte) 0x12;
    }

    public void setTimeout(short timeout) {
        mTimeout = timeout;
    }

    public short getTimeout() {
        return mTimeout;
    }

    public void setGroupNo(byte address) {
        mGroupNo = address;
    }

    public byte getGroupNo() {
        return mGroupNo;
    }

}
