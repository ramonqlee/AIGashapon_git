package android_serialport_api.vmc.coinhopper;

import android.util.Log;

import com.idreems.openvm.utils.LogUtil;

import org.apache.http.util.ByteArrayBuffer;

import java.util.ArrayList;
import java.util.List;

import android_serialport_api.serialcomm.RSReceiver;
import android_serialport_api.serialcomm.SerialComm;

/**
 * Created by ramonqlee on 7/6/16.
 */
public class CHManager implements RSReceiver {
    private static final String TAG = "CHManager";
    private static final int BUFFER_SCALER = 2;//buffer的缩放因子

    private SerialComm mSerialComm;
    private CHDataDispatcher mDispatcher;

    private int mMaxCmdLength;           //最长的指令长度
    private ByteArrayBuffer mByteBuffer; //缓冲池

    private static final CHManager sSharedInstance = new CHManager();
    private List<Ack.AckCallback> mListeners = new ArrayList<Ack.AckCallback>();

    public static CHManager sharedInstance(SerialComm serialComm) {
        sSharedInstance.init(serialComm);
        return sSharedInstance;
    }

    // 发送出币指令
    public boolean sendAsync(byte[] stream) {
        if (null == mSerialComm) {
            return false;
        }

        StringBuilder builder = new StringBuilder();
        builder.append("sendCmd: ");
        for (byte item:stream) {
            builder.append(String.format("%02X",item));
            builder.append(" ");
        }
        LogUtil.d(TAG,builder.toString());

        return mSerialComm.sendAsync(stream);
    }

    // 设置指令回调：
    public void addListener(Ack.AckCallback l) {
        if (null == l) {
            return;
        }
        if (-1 != mListeners.indexOf(l)) {
            return;
        }
        mListeners.add(l);
    }

    public void removeListener(Ack.AckCallback l) {
        if (null == l) {
            return;
        }
        mListeners.remove(l);
    }

    // TODO 机器状态定时监控
    private void startStateMonitor() {
    }

    private void init(SerialComm serialComm) {
        mSerialComm = serialComm;

        // 注册数据器，并进行分发
        if (null != mDispatcher) {
            return;
        }
        mDispatcher = CHDataDispatcher.sharedInstance();

        //添加识别器

        // 添加回应识别器
        Ack ack = new Ack();
        ack.setAckCallback(new Ack.AckCallback() {
            @Override
            public void onStatus(byte sn, byte status, byte address,byte lowDispense,byte highDespense) {
                notifyCallback(sn, status, address,lowDispense,highDespense);
            }
        });
        mDispatcher.addRecognizer(ack);

        mMaxCmdLength = Math.max(mMaxCmdLength, ack.messageLength());

        // 添加状态识别器
        Status status = new Status();
        status.setAckCallback(new Ack.AckCallback() {
            @Override
            public void onStatus(byte sn, byte status, byte address,byte lowDispense,byte highDespense) {
                notifyCallback(sn, status, address,lowDispense,highDespense);
            }
        });
        mDispatcher.addRecognizer(status);

        mMaxCmdLength = Math.max(mMaxCmdLength, status.messageLength());

        // 设置缓冲池的大小
        mByteBuffer = new ByteArrayBuffer(mMaxCmdLength * BUFFER_SCALER);

        // 缓冲池已经准备完毕，可以接收数据了
        if (null != mSerialComm) {
            mSerialComm.addDataReceiver(this);
        }
    }

    private void notifyCallback(byte sn, byte status, byte address,byte lowDispense,byte highDespense) {
        if (null == mListeners) {
            return;
        }

        for (int i = mListeners.size() - 1; i >= 0; i--) {
            Ack.AckCallback callback = mListeners.get(i);
            if (null == callback) {
                continue;
            }
            callback.onStatus(sn, status, address,lowDispense,highDespense);
        }
    }

    // 接收数据
    public void onReceive(byte[] data, int size) {
        if (null == mDispatcher) {
            return;
        }
//        Sending01010101Activity.dumpBinary(data, size, TAG);

//        从前向后，逐个识别，删除
//        0. 如果不超过buffer最大容量，则将buffer灌满，然后进入1，处理后，再返回0，依次灌满，进行处理，直至所有的数据均被灌入
//        1. 一个识别了，直接删除
//        2. 都没识别
//        2.1 有待灌入的数据，则尝试灌入，然后跳转到1进行识别
//        2.2 没有待灌入的数据，按照最长的指令长度，保留最大可能的数据
        final int bufferCapacity = mByteBuffer.capacity();
        byte left[] = new byte[size];
        System.arraycopy(data, 0, left, 0, left.length);

        do {
            //有数据待灌入,则尝试灌入缓冲池中
            if (null != left) {
                if (mByteBuffer.length() < bufferCapacity) {
                    if (bufferCapacity - mByteBuffer.length() > left.length) {//全部灌入
                        mByteBuffer.append(left, 0, left.length);
                        left = null;
                    } else {//灌入部分数据
                        final int c = bufferCapacity - mByteBuffer.length();
                        mByteBuffer.append(left, 0, c);

                        //剩余的保留
                        if (0 == left.length - c)
                        {
                            left = null;
                        }
                        else {
                            byte[] newleft = new byte[left.length - c];
                            System.arraycopy(left, c, newleft, 0, newleft.length);
                            left = newleft;
                        }
                    }
                }
            }

            // 开始处理数据
            final byte byteBuffer[] = mByteBuffer.toByteArray();
            CHDataDispatcher.Recoginizer r = mDispatcher.dispatch(byteBuffer);

            //有识别的
            if (null != r) {
                Log.d(TAG,"recognizer name="+r.name());

                int pos = r.handle(byteBuffer);
                if (pos >= byteBuffer.length)
                {
                    mByteBuffer.clear();
                }
                else {
                    byte newBytes[] = new byte[byteBuffer.length - pos];
                    System.arraycopy(byteBuffer, pos, newBytes, 0, newBytes.length);
                    mByteBuffer.clear();
                    mByteBuffer.append(newBytes, 0, newBytes.length);
                }
                continue;
            }

            // 都被没识别出来,如果没有left数据待灌入的话，直接丢弃没用的数据，跳出；否则，灌入数据，继续数据处理
            if (null != left) {
                continue;
            }

            // 丢弃最大可能的数据，然后跳出循环(仅保留最后的最大可能数据)
            if (mByteBuffer.length() <= mMaxCmdLength) {
                break;
            }

            byte newBytes[] = new byte[mMaxCmdLength];
            int pos = mByteBuffer.length() - mMaxCmdLength;

            System.arraycopy(byteBuffer, pos, newBytes, 0, newBytes.length);
            mByteBuffer.clear();
            mByteBuffer.append(newBytes, 0, newBytes.length);
            break;
        } while (true);
    }
}
