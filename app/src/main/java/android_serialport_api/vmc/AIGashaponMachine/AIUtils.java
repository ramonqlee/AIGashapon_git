package android_serialport_api.vmc.AIGashaponMachine;

/**
 * Created by ramonqlee on 11/05/2017.
 */

public class AIUtils {
    public static short checkSum(byte[] stream, int len) {
        // 计算校验和
        short crc = 0;
        short current;
        for (short i = 0; i < len; i++) {
            current = (short) (stream[i] << 8);
            for (short j = 0; j < 8; j++) {
                if ((short) (crc ^ current) < 0)
                    crc = (short) ((crc << 1) ^ 0x1221);
                else
                    crc <<= 1;
                current <<= 1;
            }
        }
        return crc;
    }

    public static int indexOf(byte[] stream, final byte val) {
        int NOMATCH = -1;
        if (null == stream || 0 == stream.length) {
            return NOMATCH;
        }
        for (int i = 0; i < stream.length; ++i) {
            if (val == stream[i]) {
                return i;
            }
        }

        return NOMATCH;
    }
}
