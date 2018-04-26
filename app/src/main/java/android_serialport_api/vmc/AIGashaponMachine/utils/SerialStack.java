package android_serialport_api.vmc.AIGashaponMachine.utils;

import com.idreems.openvm.constant.Consts;
import com.idreems.openvm.utils.LogUtil;

import org.apache.http.util.ByteArrayBuffer;

import android_serialport_api.serialcomm.RSReceiver;
import android_serialport_api.serialcomm.SerialComm;

/**
 * Created by ramonqlee on 17/05/2017.
 */

public class SerialStack implements RSReceiver {
    private static final String TAG = SerialStack.class.getSimpleName();
    private static final int BUFFER_SCALER = 2;//buffer的缩放因子

    private SerialComm mSerialComm;
    private DataDispatcher mDataDispatcher;

    private int mMaxCmdLength = 0xff;           //最长的指令长度
    private ByteArrayBuffer mByteBuffer; //缓冲池

    private static final SerialStack sSharedInstance = new SerialStack();

    public static SerialStack sharedInstance(SerialComm serialComm) {
        sSharedInstance.init(serialComm);
        return sSharedInstance;
    }

    public void setDataDispatcher(DataDispatcher dispatcher) {
        mDataDispatcher = dispatcher;
    }

    // 发送出币指令
    public boolean sendAsync(byte[] stream) {
        if (null == mSerialComm) {
            return false;
        }

        return mSerialComm.sendAsync(stream);
    }

    public void setMaxBufferLen(int len) {
        mMaxCmdLength = len;
    }

    private void init(SerialComm serialComm) {
        mSerialComm = serialComm;

        // 设置缓冲池的大小
        mByteBuffer = new ByteArrayBuffer(mMaxCmdLength * BUFFER_SCALER);

        // 缓冲池已经准备完毕，可以接收数据了
        if (null != mSerialComm) {
            mSerialComm.addDataReceiver(this);
        }
    }

    // 接收数据
    public void onReceive(byte[] data, int size) {
        if (null == mDataDispatcher || size <= 0) {
            return;
        }
//        从前向后，逐个识别，删除
//        0. 如果不超过buffer最大容量，则将buffer灌满，然后进入1，处理后，再返回0，依次灌满，进行处理，直至所有的数据均被灌入
//        1. 一个识别了，直接删除
//        2. 都没识别
//        2.1 有待灌入的数据，则尝试灌入，然后跳转到1进行识别
//        2.2 没有待灌入的数据，按照最长的指令长度，保留最大可能的数据
        final int bufferCapacity = mByteBuffer.capacity();
        byte left[] = new byte[size];
        System.arraycopy(data, 0, left, 0, left.length);
        LogUtil.d(Consts.LOG_TAG, "...........................start to recognize data..........................");
        LogUtil.d(Consts.LOG_TAG, "onReceive data = " + LogUtil.printHexString(TAG, left));

        do {
            //有数据待灌入,则尝试灌入缓冲池中
            if (null != left) {
                if (mByteBuffer.length() < bufferCapacity) {
                    LogUtil.d(Consts.LOG_TAG, "mByteBuffer.length() = " + mByteBuffer.length() + " bufferCapacity=" + bufferCapacity);
                    if (bufferCapacity - mByteBuffer.length() > left.length) {//全部灌入
                        mByteBuffer.append(left, 0, left.length);
                        left = null;
                    } else {//灌入部分数据
                        final int c = bufferCapacity - mByteBuffer.length();
                        mByteBuffer.append(left, 0, c);

                        //剩余的保留
                        if (0 == left.length - c) {
                            left = null;
                        } else {
                            byte[] newleft = new byte[left.length - c];
                            System.arraycopy(left, c, newleft, 0, newleft.length);
                            left = newleft;
                        }
                    }
                }
            }

            // 开始处理数据
            final byte byteBuffer[] = mByteBuffer.toByteArray();
            DataDispatcher.Dispatcher r = mDataDispatcher.dispatch(byteBuffer);

            LogUtil.d(Consts.LOG_TAG, "data to be recognized = " + LogUtil.printHexString(TAG, byteBuffer) + " left=" + left);
            //有识别的
            if (null != r) {
                int pos = r.handle(byteBuffer);
                if (pos >= byteBuffer.length) {
                    mByteBuffer.clear();
                } else {
                    byte newBytes[] = new byte[byteBuffer.length - pos];
                    System.arraycopy(byteBuffer, pos, newBytes, 0, newBytes.length);
                    mByteBuffer.clear();
                    mByteBuffer.append(newBytes, 0, newBytes.length);
                }
                LogUtil.d(TAG, "recognizer name=" + r.name() + " pos=" + pos);
                continue;
            }

            LogUtil.d(Consts.LOG_TAG, "data after recognized = " + LogUtil.printHexString(TAG, mByteBuffer.toByteArray()) + " left=" + left);
            // 都被没识别出来,如果没有left数据待灌入的话，直接丢弃没用的数据，跳出；否则，灌入数据，继续数据处理
            if (null != left) {
                continue;
            }

            // 丢弃最大可能的数据，然后跳出循环(仅保留最后的最大可能数据)
            if (mByteBuffer.length() <= mMaxCmdLength) {
                LogUtil.d(Consts.LOG_TAG, "no enough data,break");
                LogUtil.d(Consts.LOG_TAG, "...........................recognize data end..........................");
                break;
            }

            byte newBytes[] = new byte[mMaxCmdLength];
            int pos = mByteBuffer.length() - mMaxCmdLength;

            System.arraycopy(byteBuffer, pos, newBytes, 0, newBytes.length);
            mByteBuffer.clear();
            mByteBuffer.append(newBytes, 0, newBytes.length);
            LogUtil.d(Consts.LOG_TAG, "too much data after recognized = " + LogUtil.printHexString(TAG, mByteBuffer.toByteArray()));
            LogUtil.d(Consts.LOG_TAG, "...........................recognize data end..........................");
            break;
        } while (true);
    }

}
