package android_serialport_api.vmc.AIGashaponMachine;

import com.idreems.openvm.utils.ByteUtil;

/**
 * Created by ramonqlee on 10/05/2017.
 */

public class LockCommand extends ControlLock {
    public byte controlTypeCode() {
        return (byte) 0x1;
    }

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
        byte len = 6 + 2;
        byte[] r = new byte[len+Instruction.CHECKSUM_LEN];

        int offset = 0;
        r[offset++] = Command.FLAG;
        r[offset++] = len;
        r[offset++] = getBusAddress();
        r[offset++] = getCode();
        r[offset++] = getBusAddress();
        r[offset++] = (byte) 0;

        r[offset++] = controlTypeCode();
        r[offset++] = getGroupNo();

        short checkSum = AIUtils.checkSum(r, offset);
        r[offset++] = ByteUtil.HIBYTE(checkSum);
        r[offset] = ByteUtil.LOBYTE(checkSum);

        return r;
    }
}
