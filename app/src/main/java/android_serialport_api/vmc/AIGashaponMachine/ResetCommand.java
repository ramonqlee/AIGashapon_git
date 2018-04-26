package android_serialport_api.vmc.AIGashaponMachine;

/**
 * Created by ramonqlee on 10/05/2017.
 */

public class ResetCommand extends LockCommand {
    public byte controlTypeCode() {
        return (byte) 0x2;
    }

    @Override
    public byte[] getBytes() {
        return super.getBytes();
    }
}
