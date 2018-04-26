package android_serialport_api.vmc.AIGashaponMachine;

/**
 * Created by ramonqlee on 10/05/2017.
 */

public interface Report {
    public static final int UNKNOWN = -1;
    public static final byte FLAG = (byte) 0xC8;

    String name();
    int handle(byte[] stream);
}
