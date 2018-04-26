package android_serialport_api.vmc.AIGashaponMachine;

import com.idreems.openvm.constant.Consts;
import com.idreems.openvm.utils.ByteUtil;
import com.idreems.openvm.utils.LogUtil;

/**
 * Created by ramonqlee on 10/05/2017.
 */

public class StatusReport extends Location implements Report, Instruction {
    private static final int S0_LEN = 4;
    private static final int STATE_LEN = 3;

    public static final byte STATE_OFF = 0;
    public static final byte STATE_ON = 1;
    private byte[] state1;// 第一组的状态
    private byte[] state2;// 第二组的状态
    private byte[] state3;// 第三组的状态

    public byte[] getState1() {
        return state1;
    }

    public byte[] getState2() {
        return state2;
    }

    public byte[] getState3() {
        return state3;
    }

    private byte getState(byte group, int pos) {
        byte r = 0;
        switch (group) {
            case 1:
                r = (STATE_LEN == state1.length) ? state1[pos] : STATE_OFF;
                break;
            case 2:
                r = (STATE_LEN == state2.length) ? state2[pos] : STATE_OFF;
                break;
            case 3:
                r = (STATE_LEN == state3.length) ? state3[pos] : STATE_OFF;
                break;
        }
        return r;
    }

    /**
     * @param group 1-3
     * @return
     */
    public boolean isLockOpen(byte group) {
        return STATE_ON == getState(group, 0);
    }

    public boolean isRotated(byte group) {
        return STATE_ON == getState(group, 2);
    }

    public boolean isCheckingOn(byte group) {
        return STATE_ON == getState(group, 1);
    }

    @Override
    public byte getCode() {
        return (byte) 0x91;
    }

    @Override
    public String name() {
        return "查询扭蛋机状态";
    }

    @Override
    public int handle(byte[] stream) {
        final int NOMATCH = Report.UNKNOWN;
        try {
            final int protocoLength = 8 + 9 + 4;
            //  定位开始标志
            // 开始标识 (1)
            final int offset = AIUtils.indexOf(stream, Report.FLAG);
            LogUtil.d(Consts.LOG_TAG, "StatusReport offset= "+offset);
            if (offset < 0) {
                return NOMATCH;
            }

            // 消息长度（1）
            if (stream.length < offset + protocoLength) {
                return offset;
            }
            final byte messageLen = stream[offset + Instruction.LEN_POS];//消息长度
            LogUtil.d(Consts.LOG_TAG, "StatusReport messageLen= "+messageLen);
            if (messageLen != protocoLength - Instruction.CHECKSUM_LEN) {
                return NOMATCH;
            }

            // 柜子地址 (1)
            if (stream[offset + Instruction.ADDR_POS] < Location.MIN_BUS_ADDRESS || stream[offset + Instruction.ADDR_POS] > Location.MAX_BUS_ADDRESS) {
                LogUtil.d(Consts.LOG_TAG, "StatusReport illegal address");
                return NOMATCH;
            }

            // 消息类型 (1)
            if (stream[offset + Instruction.MT_POS] != getCode()) {
                LogUtil.d(Consts.LOG_TAG, "not StatusReport type");
                return NOMATCH;
            }

            // 柜子地址 (1)
            setBusAddress(stream[offset + Instruction.ADDR_POS]);
            // 未定义   (1)
            int cursor = offset + Instruction.UNDEF_POS;
            // 自定义数据 (9+4)
            cursor += S0_LEN;
            // 解析自定义数据
            // s0(4)+(s1+s2+s3)X3
            // 第一组状态
            state1 = new byte[STATE_LEN];
            for (int i = 0; i < STATE_LEN; ++i) {
                state1[i] = stream[++cursor];
            }

            // 第二组状态
            state2 = new byte[STATE_LEN];
            for (int i = 0; i < STATE_LEN; ++i) {
                state2[i] = stream[++cursor];
            }

            // 第三组状态
            state3 = new byte[STATE_LEN];
            for (int i = 0; i < STATE_LEN; ++i) {
                state3[i] = stream[++cursor];
            }

            // 校验和   (2)
            short checkSum = AIUtils.checkSum(stream, offset + protocoLength - Instruction.CHECKSUM_LEN);
            if (ByteUtil.HIBYTE(checkSum) != stream[offset + protocoLength - Instruction.CHECKSUM_LEN] || ByteUtil.LOBYTE(checkSum) != stream[offset + protocoLength - Instruction.CHECKSUM_LEN + 1]) {
                LogUtil.d(Consts.LOG_TAG, "StatusReport illegal checksum");
                return NOMATCH;
            }

            return offset + protocoLength;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return NOMATCH;
    }
}
