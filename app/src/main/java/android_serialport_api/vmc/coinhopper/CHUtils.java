package android_serialport_api.vmc.coinhopper;

/**
 * Created by ramonqlee on 7/5/16.
 */
public class CHUtils {

    public static byte LOBYTE(short v) {
        return (byte) (0xff & v);
    }

    public static byte HIBYTE(short v) {
        return (byte) ((0xff00 & v) >> 8);
    }

    public static byte xor(byte[] stream, int size) {
        if (null == stream || stream.length < size) {
            return 0;
        }

        byte r = stream[0];
        for (int i = 1; i < size; ++i) {
            r ^= stream[i];
        }
        return r;
    }
}
