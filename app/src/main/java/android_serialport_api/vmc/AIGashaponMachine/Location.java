package android_serialport_api.vmc.AIGashaponMachine;

/**
 * Created by ramonqlee on 10/05/2017.
 */

public abstract class Location {
    public static final byte BUS_ADDRESS_OFFSET = 1;

    public static final byte MIN_BUS_ADDRESS = 0;
    public static final byte MAX_BUS_ADDRESS = 31;

    public static final byte ALL_BUS_ADDRESS = (byte)0xff;

    protected byte mBusAddress;//总线地址

    public void setBusAddress(byte address) {
        mBusAddress = address;
    }

    public byte getBusAddress() {
        return mBusAddress;
    }
}
