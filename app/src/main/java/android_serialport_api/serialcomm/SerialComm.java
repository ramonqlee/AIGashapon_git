package android_serialport_api.serialcomm;

import android.serialport.SerialPort;
import android.text.TextUtils;

import com.idreems.openvm.constant.Consts;
import com.idreems.openvm.utils.LogUtil;

import org.apache.http.util.ByteArrayBuffer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidParameterException;
import java.util.Vector;


/**
 * Created by ramonqlee on 5/22/16.
 * 处理流程：数据首先进入写入队列，然后进行发送，完毕后，等待返回数据，然后进入下一个写入周期
 */
public class SerialComm implements RSReceiver {
    private static final String TAG = SerialComm.class.getSimpleName();
    private static final int MAX_INST_LOOP_TIME_OUT = 5 * 1000;//单个指令发出后，相应允许的超时时间，从发送完毕开始计算,到收到第一个返回计算

    private static SerialComm sSerialComm = new SerialComm();

    // 接收到数据后的数据通知对象
    private Vector<RSReceiver> mReceiverVector = new Vector<RSReceiver>();

    // rs参数
    private String mDevicePath;
    private int mBaudRate;
    private int mInstLoopTimeoutInMills = MAX_INST_LOOP_TIME_OUT;//两次写数据的间隔时间。如果为0，则一直等待

    //rs数据处理接口
    protected SerialPort mSerialPort;
    protected OutputStream mOutputStream;
    private InputStream mInputStream;
    private ReadThread mReadThread;
    private SendingThread mSendingThread;

    // 发送指令队列
    private Vector<ByteArrayBuffer> mToSendBytesList = new Vector<ByteArrayBuffer>();

    // 写入数据的同步锁
    private final Object mSendLock = new Object();

    public static SerialComm sharedInstace() {
        return sSerialComm;
    }

    // 串口通讯参数配置
    public SerialComm config(String devicePath, int baudRate) {
        mDevicePath = devicePath;
        mBaudRate = baudRate;
        return this;
    }

    public SerialComm setSendTimeOut(int timeOutInMills) {
        mInstLoopTimeoutInMills = timeOutInMills;
        return this;
    }

    public boolean sendAsync(byte[] buffer) {
        if (null == buffer || 0 == buffer.length) {
            return false;
        }

        // 发送的队列
        ByteArrayBuffer newByreBuffer = new ByteArrayBuffer(buffer.length);
        newByreBuffer.append(buffer, 0, buffer.length);
        if (null == mToSendBytesList) {
            mToSendBytesList = new Vector<ByteArrayBuffer>();
        }
        mToSendBytesList.add(newByreBuffer);

        if (mSerialPort != null && (null == mSendingThread || !mSendingThread.isAlive())) {
            mSendingThread = new SendingThread();
            mSendingThread.start();
        }

        return true;
    }

    public void addDataReceiver(RSReceiver receiver) {
        if (null == receiver) {
            return;
        }
        if (null == mReceiverVector) {
            mReceiverVector = new Vector<RSReceiver>();
        }
        if (mReceiverVector.contains(receiver)) {
            return;
        }

        mReceiverVector.add(receiver);
    }

    public void removeDataReceiver(RSReceiver receiver) {
        if (null == receiver) {
            return;
        }
        if (null == mReceiverVector) {
            mReceiverVector = new Vector<RSReceiver>();
        }
        mReceiverVector.remove(receiver);
    }

    public void clearDataReceivers() {
        if (null == mReceiverVector) {
            return;
        }
        mReceiverVector.clear();
        mReceiverVector = null;
    }

    public boolean start() {
        // open serial port and wait for data coming
        try {
            //close serial port if opened before
            closeSerialPort();
            mSerialPort = getSerialPort(mDevicePath, mBaudRate);

            if (null == mSerialPort) {
                return false;
            }
            mOutputStream = mSerialPort.getOutputStream();
            mInputStream = mSerialPort.getInputStream();

			/* Create a receiving thread */
            if (null == mReadThread || !mReadThread.isAlive()) {
                mReadThread = new ReadThread();
                mReadThread.start();
                LogUtil.d(TAG, "mReadThread.start");
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public void stop() {
        if (null != mReadThread) {
            mReadThread.interrupt();
        }
        if (null != mSendingThread) {
            mSendingThread.interrupt();
        }

        if (null != mSerialPort) {
            closeSerialPort();
        }
    }

    public void onReceive(byte[] data, int size) {
        // 通知接收者，接收数据
        if (null == mReceiverVector || 0 == mReceiverVector.size()) {
            return;
        }
        for (RSReceiver receiver : mReceiverVector) {
            receiver.onReceive(data, size);
        }

        //通知进入下一个数据的写入
        synchronized (mSendLock) {
            mSendLock.notify();
        }
    }

    private class ReadThread extends Thread {

        @Override
        public void run() {
            super.run();
            while (!isInterrupted()) {
                int size;
                try {
                    byte[] buffer = new byte[128];
                    if (mInputStream == null) return;
                    LogUtil.d(TAG, ".....................ReadThread wait to read data.....................");
                    size = mInputStream.read(buffer);
                    LogUtil.d(TAG, "ReadThread read data = " + LogUtil.printHexString(Consts.LOG_TAG, buffer));
                    if (size > 0) {
                        onReceive(buffer, size);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            }
        }
    }

    // 发送数据
    private class SendingThread extends Thread {
        @Override
        public void run() {
            while (!isInterrupted()) {
                synchronized (mSendLock) {
                    try {
                        if (null == mOutputStream) {
                            return;
                        }

                        //发送完毕或者根本没有数据，直接等下次数据来了再发送
                        if (null != mToSendBytesList && 0 != mToSendBytesList.size()) {
                            ByteArrayBuffer top = mToSendBytesList.remove(0);
                            if (null == top) {
                                continue;
                            }
                            byte[] buffer = top.buffer();
                            if (null == buffer || 0 == buffer.length) {
                                continue;
                            }

                            mOutputStream.write(buffer);
                            LogUtil.d(Consts.LOG_TAG, "SendingThread write data = " + LogUtil.printHexString(TAG, buffer));
                        }

                        try {
                            // 超时处理,防止卡在这个地方,走不下去
                            // 现在的流程时，在接收到数据的地方，会通知写入线程继续走下去
                            // 如果出现异常，则靠这个继续走下去
                            mSendLock.wait(mInstLoopTimeoutInMills);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }
                }
            }
        }
    }


    //  串口操作
    private SerialPort getSerialPort(String path, int baudrate) throws SecurityException, IOException, InvalidParameterException {
        if (mSerialPort == null) {
            /* Check parameters */
            if ((path.length() == 0) || (baudrate == -1)) {
                throw new InvalidParameterException();
            }

			/* Open the serial port */
            mSerialPort = new SerialPort(new File(path), baudrate, 0);
        }
        return mSerialPort;
    }

    private void closeSerialPort() {
        if (mSerialPort != null) {
            mSerialPort.close();
            mSerialPort = null;
        }
    }


    // 创建共享的串口通讯，coin hopper专用对象
    public static SerialComm createSerialComm(String devicePath, int baudRate) {
        if (TextUtils.isEmpty(devicePath) || baudRate <= 0) {
            return null;
        }

        // 配置串口参数，并启动串口通讯
        try {
            SerialComm serialComm = SerialComm.sharedInstace();
            serialComm.config(devicePath, baudRate);
            LogUtil.d(LogUtil.LOG_TAG, "devicePath =" + devicePath + " baudRate=" + baudRate);
            if (!serialComm.start()) {
                LogUtil.e(LogUtil.LOG_TAG, "fail to connect serial");
                return null;
            }
            return serialComm;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
}
