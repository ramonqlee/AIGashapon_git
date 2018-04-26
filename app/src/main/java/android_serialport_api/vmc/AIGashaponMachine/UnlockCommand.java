package android_serialport_api.vmc.AIGashaponMachine;

import com.idreems.openvm.utils.ByteUtil;

/**
 * Created by ramonqlee on 10/05/2017.
 */

public class UnlockCommand extends ControlLock {

    public byte controlTypeCode() {
        return LockerReport.OPEN;
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
        final byte len = 6 + 4;
        byte[] r = new byte[len + Instruction.CHECKSUM_LEN];

        int offset = 0;
        r[offset++] = Command.FLAG;
        r[offset++] = len;
        r[offset++] = getBusAddress();
        r[offset++] = getCode();
        r[offset++] = getBusAddress();
        r[offset++] = (byte) 0;

        r[offset++] = controlTypeCode();
        r[offset++] = getGroupNo();
        r[offset++] = ByteUtil.HIBYTE(getTimeout());
        r[offset++] = ByteUtil.LOBYTE(getTimeout());

        short checkSum = AIUtils.checkSum(r, offset);
        r[offset++] = ByteUtil.HIBYTE(checkSum);
        r[offset] = ByteUtil.LOBYTE(checkSum);

        return r;
    }
}
