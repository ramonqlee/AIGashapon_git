package android_serialport_api.vmc.AIGashaponMachine.utils;

import android.text.TextUtils;

import java.util.List;

import android_serialport_api.serialcomm.SerialComm;
import android_serialport_api.vmc.AIGashaponMachine.Report;
import android_serialport_api.vmc.RS232;

/**
 * Created by ramonqlee on 17/05/2017.
 * 智能扭蛋机
 */

public class AIGashpon {
    private static AIGashpon sAIGashpon = new AIGashpon();

    private SerialStack mSerialStack;
    DataDispatcher mDataDispatcher = new DataDispatcher();

    public static AIGashpon sharedInstance(SerialComm serialComm) {
        if (null == sAIGashpon.mSerialStack) {
            sAIGashpon.mSerialStack = SerialStack.sharedInstance(serialComm);
            sAIGashpon.mSerialStack.setDataDispatcher(sAIGashpon.mDataDispatcher);
        }

        return sAIGashpon;
    }

    private AIGashpon() {
    }

    /**
     * 发送指令
     *
     * @param cmd
     */
    public void send(RS232 cmd) {
        if (null == mSerialStack || null == cmd) {
            return;
        }

        byte[] r = cmd.getBytes();
        mSerialStack.sendAsync(r);
    }

    /**
     * 添加状态报告监控
     * 同一个类型的，只保留最后添加的一个
     *
     * @param report
     */
    public void addReporter(final Report report) {
        if (null == report) {
            return;
        }

        // 同一个类型的，只添加一个;对于重复的，先移除之前的
        removeReporter(report);

        DataDispatcher.Dispatcher dispatcher = new DataDispatcher.Dispatcher() {
            @Override
            public String name() {
                return report.name();
            }

            @Override
            public int handle(byte[] stream) {
                return report.handle(stream);
            }
        };
        mDataDispatcher.addRecognizer(dispatcher);
    }

    /**
     * 移除状态报告监控
     *
     * @param report
     */
    public void removeReporter(Report report) {
        if (null == report) {
            return;
        }
        // 同一个类型的，只添加一个;对于重复的，先移除之前的
        List<DataDispatcher.Dispatcher> list = mDataDispatcher.getRegList();
        for (int i = list.size() - 1; i >= 0; i--) {
            DataDispatcher.Dispatcher temp = list.get(i);
            if (null == temp || TextUtils.equals(temp.name(), report.name())) {
                list.remove(i);
            }
        }
    }

    /**
     * 清空状态报告监控
     */
    public void clearReporter() {
        mDataDispatcher.clear();
    }
}
