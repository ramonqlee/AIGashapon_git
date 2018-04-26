package android_serialport_api.vmc.AIGashaponMachine;

import com.idreems.openvm.utils.ByteUtil;

import android_serialport_api.vmc.RS232;

/**
 * Created by ramonqlee on 10/05/2017.
 */

public class GetStatus extends Location implements RS232, Command, Instruction {
    @Override
    public byte[] getBytes() {
        // 开始标识 (1)
        // 消息长度（1）
        // 柜子地址 (1)
        // 消息类型 (1)
        // 柜子地址 (1)
        // 未定义   (1)
        // 自定义数据 (0-249)
        // 校验和   (2)
        final byte dataLen = 6 + 0;
        final byte arraylen = dataLen + 2;
        byte[] r = new byte[arraylen];

        int offset = 0;
        r[offset++] = Command.FLAG;
        r[offset++] = dataLen;
        r[offset++] = getBusAddress();
        r[offset++] = getCode();
        r[offset++] = getBusAddress();
        r[offset++] = (byte) 0;

        short checkSum = AIUtils.checkSum(r, offset);
        r[offset++] = ByteUtil.HIBYTE(checkSum);
        r[offset] = ByteUtil.LOBYTE(checkSum);

        return r;
    }

    @Override
    public byte getCode() {
        return (byte) 0x11;
    }
}
