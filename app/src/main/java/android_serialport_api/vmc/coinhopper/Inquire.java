package android_serialport_api.vmc.coinhopper;

/**
 * Created by ramonqlee on 7/5/16.
 */
public class Inquire extends CHProtocolBase {
    public byte[] getBytes() {
        byte[] ret = new byte[8];
        int c = 0;
        ret[c++] = header();
        ret[c++] = messageLength();
        ret[c++] = (byte)getPayoutSN();
        ret[c++] = commandCode();
        ret[c++] = getCOMAddress();
        ret[c++] = 0;
        ret[c++] = 0;
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
        return (byte) 0x51;
    }
}
