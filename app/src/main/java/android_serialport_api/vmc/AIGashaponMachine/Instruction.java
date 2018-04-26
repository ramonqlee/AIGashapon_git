package android_serialport_api.vmc.AIGashaponMachine;

/**
 * Created by ramonqlee on 10/05/2017.
 */

public interface Instruction {
    public static final byte SF_POS = 0;
    public static final byte LEN_POS = 1;
    public static final byte ADDR_POS = 2;
    public static final byte MT_POS = 3;
    public static final byte ADDR_POS_2 = 4;
    public static final byte UNDEF_POS = 5;

    public static final byte CHECKSUM_LEN = 2;

    public static final byte GROUP_COUNT_PER_CHIP = 3;//每块板子可以带几个扭蛋机
    public static final byte STATE_COUNT_PER_GASHPON = 3;//每个扭蛋机的状态

    byte getCode();
}
