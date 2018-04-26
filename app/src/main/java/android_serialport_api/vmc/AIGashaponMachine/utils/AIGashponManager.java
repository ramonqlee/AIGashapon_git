package android_serialport_api.vmc.AIGashaponMachine.utils;

import com.idreems.openvm.constant.Consts;
import com.idreems.openvm.utils.LogUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import android_serialport_api.serialcomm.SerialComm;
import android_serialport_api.vmc.AIGashaponMachine.GetStatus;
import android_serialport_api.vmc.AIGashaponMachine.Instruction;
import android_serialport_api.vmc.AIGashaponMachine.Location;
import android_serialport_api.vmc.AIGashaponMachine.LockCommand;
import android_serialport_api.vmc.AIGashaponMachine.LockerReport;
import android_serialport_api.vmc.AIGashaponMachine.Report;
import android_serialport_api.vmc.AIGashaponMachine.ResetCommand;
import android_serialport_api.vmc.AIGashaponMachine.StatusReport;
import android_serialport_api.vmc.AIGashaponMachine.UnlockCommand;

/**
 * Created by ramonqlee on 25/05/2017.
 * 只能扭蛋机工控的抽象
 * 管理多组扭蛋机，每组扭蛋机最多有三个，每个具体的扭蛋机通过总线地址和组别号区分
 */

public class AIGashponManager {
    private static final long CHECK_STATE_INTERVAL = 5 * 1000;//定时查询状态的间隔,轮训所有扭蛋机

    private static AIGashponManager sAIGashponManager = new AIGashponManager();
    private static AIGashpon sAIGashpon;

    private List<StatusReportListener> mStatusReportListeners = new ArrayList<>();
    private List<LockerReportListener> mLockerReportListeners = new ArrayList<>();

    private Timer mLooperTimer;
    private byte[][] mStates = new byte[Location.MAX_BUS_ADDRESS - Location.MIN_BUS_ADDRESS + 1][Instruction.GROUP_COUNT_PER_CHIP * Instruction.STATE_COUNT_PER_GASHPON];//所有板子的扭蛋机状态

    private Report mInnerStatusReportListener;
    private Report mInnerLockerReportListener;

    private static final Byte UNCHECKED = 0;
    private static final Byte CHECKING = 1;
    private static final Byte CHECKED = 2;
    private HashMap<Byte, Byte> mMonitorAddresseMap = new HashMap<>();//监控地址和查询状态

    private static final byte MAX_FAIL_COUNT = (byte) (5 + 60 * 1000 / CHECK_STATE_INTERVAL);//查询失败的最大次数
    private HashMap<Byte, Byte> mMonitorAddressFailsMap = new HashMap<>();//监控地址和查询状态的失败次数

    private AIGashponManager() {
    }

    public static AIGashponManager sharedInstance(String devicePath, int baudRate) {
        sAIGashponManager.init(devicePath, baudRate);
        return sAIGashponManager;
    }

    /**
     * 是否在出货中
     *
     * @param address 总线地址
     * @param groupNo 从1开始
     * @return
     */
    public boolean isCheckingOut(byte address, byte groupNo) {
        // FIXME 首先查询上次请求的时间，如果已经超时，则直接返回false
        //  其次查询上次返回的状态
        return StatusReport.STATE_ON == mStates[address][(groupNo - 1) * Instruction.STATE_COUNT_PER_GASHPON];
    }

    // 开锁
    public void open(byte address, byte groupNo, short timeoutInSec) {
        if (null == sAIGashpon) {
            return;
        }

        UnlockCommand cmd = new UnlockCommand();
        // 待设置参数
        cmd.setBusAddress(address);
        cmd.setGroupNo(groupNo);

        cmd.setTimeout(timeoutInSec);

        sAIGashpon.send(cmd);

        // 需要启动定时器，定时查询状态
        startLoopStatus(address, CHECK_STATE_INTERVAL);
    }

    // 关锁
    public void close(byte address, byte groupNo) {
        if (null == sAIGashpon) {
            return;
        }

        LockCommand cmd = new LockCommand();
        // TODO 待设置参数
        cmd.setBusAddress(address);
        cmd.setGroupNo(groupNo);

        sAIGashpon.send(cmd);
        // 需要启动定时器，定时查询状态
        startLoopStatus(address, CHECK_STATE_INTERVAL);
    }

    // 重置
    public void reset(byte address, byte groupNo) {
        if (null == sAIGashpon) {
            return;
        }
        ResetCommand cmd = new ResetCommand();
        // TODO 待设置参数
        cmd.setBusAddress(address);
        cmd.setGroupNo(groupNo);

        sAIGashpon.send(cmd);
        // 需要启动定时器，定时查询状态
        startLoopStatus(address, CHECK_STATE_INTERVAL);
    }

    private void getStatus(byte address) {
        if (null == sAIGashpon) {
            return;
        }

        GetStatus getStatus = new GetStatus();
        getStatus.setBusAddress(address);

        sAIGashpon.send(getStatus);
        LogUtil.d(Consts.LOG_TAG, "start checking state,address = " + address);
    }

    private void addMonitorAddress(byte address) {
        // 添加到监控地址列表中，监控其出货状态
        synchronized (mMonitorAddresseMap) {
            if (Location.ALL_BUS_ADDRESS == address) {
                for (byte i = Location.MIN_BUS_ADDRESS; i <= Location.MAX_BUS_ADDRESS; i++) {
                    mMonitorAddresseMap.put(new Byte(i), UNCHECKED);
                    LogUtil.d(LogUtil.LOG_TAG, "addMonitorAddress, address = " + i);
                }
            } else {
                if (null != mMonitorAddresseMap) {
                    mMonitorAddresseMap.put(new Byte(address), UNCHECKED);
                    LogUtil.d(LogUtil.LOG_TAG, "addMonitorAddress, address = " + address);
                }
            }
        }
    }

    private void removeMonitorAddress(byte address) {
        synchronized (mMonitorAddresseMap) {
            if (null == mMonitorAddresseMap.remove(new Byte(address))) {
                return;
            }
        }
        LogUtil.d(LogUtil.LOG_TAG, "removed monitor, address = " + address);
    }

    private void clearMonitorAddress() {
        synchronized (mMonitorAddresseMap) {
            mMonitorAddresseMap.clear();
        }
    }


    /**
     * 定时监控指定地址的状态，直到锁关闭
     *
     * @param address
     * @param intervalInMs
     */
    private void startLoopStatus(final byte address, final long intervalInMs) {
        if (null != mLooperTimer) {
            mLooperTimer.cancel();
        }

        addMonitorAddress(address);

        mLooperTimer = new Timer();
        long delay = (intervalInMs <= 0) ? CHECK_STATE_INTERVAL : intervalInMs;
        final TimerTask r = new TimerTask() {
            @Override
            public void run() {
                // 对所有扭蛋机发起状态查询
                synchronized (mMonitorAddresseMap) {
                    Set<Byte> keys = mMonitorAddresseMap.keySet();
                    for (Byte address : keys) {
                        if (null == mMonitorAddresseMap.get(address)) {
                            continue;
                        }
                        // 检查中,暂不发起新的查询，等待
                        if (CHECKING == mMonitorAddresseMap.get(address)) {
                            // 设置查询失败的次数
                            Object obj = mMonitorAddressFailsMap.get(address);
                            if (null != obj && obj instanceof Byte) {
                                byte val = (Byte) obj;
                                if (val < MAX_FAIL_COUNT) {
                                    mMonitorAddressFailsMap.put(address, ++val);
//                                    LogUtil.d(Consts.LOG_TAG, "skip, because we are checking state,address = " + address + " fail count=" + val);
                                } else {
                                    mMonitorAddresseMap.put(address, CHECKED);
                                    mMonitorAddressFailsMap.remove(address);
                                    LogUtil.d(Consts.LOG_TAG, "reloop when time out,address = " + address + " fail count=" + val);
                                }
                            } else {
                                mMonitorAddressFailsMap.put(address, (byte) 1);
//                                LogUtil.d(Consts.LOG_TAG, "skip, because we are checking state,address = " + address + " fail count=" + 1);
                            }
                            continue;
                        }

                        // 设置为检查中
                        mMonitorAddresseMap.put(address, CHECKING);
                        getStatus(address);
                    }
                }
            }
        };
        mLooperTimer.schedule(r, delay, delay);
    }

    // 设置状态监听
    public void addStatusListener(StatusReportListener listener) {
        if (null == listener || -1 != mStatusReportListeners.indexOf(listener)) {
            return;
        }
        if (null == mStatusReportListeners) {
            return;
        }
        mStatusReportListeners.add(listener);
    }

    public void removeStatusListener(StatusReportListener listener) {
        if (null == listener) {
            return;
        }
        if (null == mStatusReportListeners) {
            return;
        }
        mStatusReportListeners.remove(listener);
    }

    public void clearStatusListener() {
        if (null == mStatusReportListeners) {
            return;
        }
        if (null == mStatusReportListeners) {
            return;
        }
        mStatusReportListeners.clear();
    }


    // 锁状态监听，只有发送开锁，关锁，重置后生效
    public void addLockListener(LockerReportListener listener) {
        if (null == listener || -1 != mLockerReportListeners.indexOf(listener)) {
            return;
        }
        if (null == mLockerReportListeners) {
            return;
        }
        mLockerReportListeners.add(listener);
    }

    public void removeLockListener(LockerReportListener listener) {
        if (null == listener) {
            return;
        }
        if (null == mLockerReportListeners) {
            return;
        }
        mLockerReportListeners.remove(listener);
    }

    public void clearLockListener() {
        if (null == mLockerReportListeners) {
            return;
        }
        mLockerReportListeners.clear();
    }

    private void init(String devicePath, int baudRate) {
        // TODO 初始化，包括串口和监听初始化
        if (null != sAIGashpon) {
            return;
        }
        SerialComm serialComm = SerialComm.createSerialComm(devicePath, baudRate);
        sAIGashpon = AIGashpon.sharedInstance(serialComm);
        sAIGashpon.addReporter(getInnerLockerReportListener());
        sAIGashpon.addReporter(getInnerStatusReportListener());

        // 需要启动定时器，定时查询状态
//        startLoopStatus(Location.ALL_BUS_ADDRESS, CHECK_STATE_INTERVAL);
    }

    private Report getInnerStatusReportListener() {
        if (null != mInnerStatusReportListener) {
            return mInnerStatusReportListener;
        }
        mInnerStatusReportListener = new Report() {
            StatusReport mStateReport = new StatusReport();

            public String name() {
                return mStateReport.name();
            }

            public int handle(byte[] stream) {
                int r = mStateReport.handle(stream);
                if (Report.UNKNOWN == r) {
                    LogUtil.d(LogUtil.LOG_TAG, "not statusReport");//added since 20180403
                    return r;
                }

                // 设置状态
                final byte address = mStateReport.getBusAddress();
                int offset = 0;
                LogUtil.d(LogUtil.LOG_TAG, "handle StatusReport,  = " + address);

                byte[] temp = mStateReport.getState1();
                for (int i = 0; i < Math.min(temp.length, Instruction.GROUP_COUNT_PER_CHIP); i++) {
                    mStates[address][offset++] = temp[i];
                }

                temp = mStateReport.getState2();
                for (int i = 0; i < Math.min(temp.length, Instruction.GROUP_COUNT_PER_CHIP); i++) {
                    mStates[address][offset++] = temp[i];
                }

                temp = mStateReport.getState3();
                for (int i = 0; i < Math.min(temp.length, Instruction.GROUP_COUNT_PER_CHIP); i++) {
                    mStates[address][offset++] = temp[i];
                }

                // 当前地址上的所有都扭蛋机都关闭了，就不再监控其状态了
                boolean allClosed = true;
                for (byte i = 0; i < Instruction.GROUP_COUNT_PER_CHIP; i++) {
                    byte groupNo = (byte) (i + 1);
                    boolean isLockOpen = mStateReport.isLockOpen(groupNo);
                    LogUtil.d(LogUtil.LOG_TAG, "handle StatusReport,isLockOpen =" + isLockOpen + " at groupNo= " + groupNo);
                    if (isLockOpen) {
                        allClosed = false;
                        break;
                    }
                }

                // 都没打开，说明无需监控了；否则继续监控
                if (allClosed) {
                    removeMonitorAddress(address);
                } else {
                    addMonitorAddress(address);
                }

                notifyStatusListener(mStateReport);
                return r;
            }
        };
        return mInnerStatusReportListener;
    }

    private Report getInnerLockerReportListener() {
        if (null != mInnerLockerReportListener) {
            return mInnerLockerReportListener;
        }
        mInnerLockerReportListener = new Report() {
            LockerReport mLockerReport = new LockerReport();

            public String name() {
                return mLockerReport.name();
            }

            public int handle(byte[] stream) {
                int r = mLockerReport.handle(stream);
                if (Report.UNKNOWN != r) {
                    notifyLockerListener(mLockerReport);
                } else {
                    LogUtil.d(LogUtil.LOG_TAG, "not lockReport");//added since 20180403
                }

                return r;
            }
        };
        return mInnerLockerReportListener;
    }

    private void notifyStatusListener(StatusReport report) {
        for (int i = mStatusReportListeners.size() - 1; i >= 0; i--) {
            StatusReportListener l = mStatusReportListeners.get(i);
            if (null == l) {
                mStatusReportListeners.remove(i);
                continue;
            }
            l.onCall(report);
        }
    }

    private void notifyLockerListener(LockerReport report) {
        for (int i = mLockerReportListeners.size() - 1; i >= 0; i--) {
            LockerReportListener l = mLockerReportListeners.get(i);
            if (null == l) {
                mLockerReportListeners.remove(i);
                continue;
            }
            l.onCall(report);
        }
    }


    public interface StatusReportListener {
        void onCall(StatusReport report);
    }

    public interface LockerReportListener {
        void onCall(LockerReport report);
    }
}
