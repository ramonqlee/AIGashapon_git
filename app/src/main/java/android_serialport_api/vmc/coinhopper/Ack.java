package android_serialport_api.vmc.coinhopper;

/**
 * Created by ramonqlee on 7/5/16.
 */
public class Ack implements CHDataDispatcher.Recoginizer{
    /**
     * Bit 7 =1: An error command received
     * Bit 6 =1: Hopper malfunction
     * Bit 5 =1: Insufficient coin storage
     * Bit 4 =1: Sensor error
     * Bit 3 =1: Coin storage empty
     * Bit 2 =1: An error data received
     * Bit 1 =1: Busy
     * Bit 0 =1: Repeated S.N. (same command received more than one time)
     */
    //status code
    public static final byte STATUS_CODE_ERROR_COMMMAND     = (byte) (0x1 << 7);
    public static final byte STATUS_CODE_HOPPER_MALFUNCTION = (byte) (0x1 << 6);
    public static final byte STATUS_CODE_INSUFFIENT_COIN    = (byte) (0x1 << 5);
    public static final byte STATUS_CODE_SENSOR_EROR        = (byte) (0x1 << 4);
    public static final byte STATUS_CODE_EMPTY_STORAGE      = (byte) (0x1 << 3);
    public static final byte STATUS_CODE_DATA_ERROR         = (byte) (0x1 << 2);
    public static final byte STATUS_CODE_BUSY               = (byte) (0x1 << 1);
    public static final byte STATUS_CODE_REPEATED_SN        = 1;

    public interface AckCallback {
        //返回序列号,状态码,com端口
         void onStatus(byte sn, byte status, byte address, byte lowDispense, byte highDispense);
    }

    private AckCallback mAckCallback;

    public void setAckCallback(AckCallback listener)
    {
        mAckCallback = listener;
    }

    @Override
    public String name()
    {
        return this.getClass().getSimpleName();
    }

    @Override
    public int handle(byte[]stream)
    {
        // 检查数据长度
        if (null == stream || messageLength() > stream.length)
        {
            return -1;
        }

        // 是否可以匹配本协议数据
        // 检查字节头，是否本协议数据
        // 定位到数据头，然后看里面的数据
        int headerPos = -1;
        for (int i= 0; i < stream.length; ++i)
        {
            if(header() == stream[i])
            {
                headerPos = i;
                break;
            }
        }

        if (headerPos < 0)
        {
            return -1;
        }

        // 数据校验，是否合法
        // 长度是否合法
        if (stream.length-headerPos<messageLength())
        {
            return -1;
        }
        // 里面的字段是否合法

        // 长度，校验等
        final int MESSAGE_LENGTH_POS = 1;
        if(stream[headerPos+MESSAGE_LENGTH_POS] != messageLength())
        {
            return -1;
        }
        final byte mysStream[] =new byte[messageLength()];
        System.arraycopy(stream,headerPos,mysStream,0,messageLength());

        byte xor = CHUtils.xor(mysStream,mysStream.length-1);//ignore original checksum
        if (xor != stream[messageLength()-1])
        {
            return -1;
        }

        final int SN_POSITION          = 2;
        final int STATUS_CODE_POSITION = 3;
        final int COM_ADDRESS_POSITION = 4;
        if (null != mAckCallback) {
            mAckCallback.onStatus(stream[SN_POSITION], stream[STATUS_CODE_POSITION], stream[COM_ADDRESS_POSITION],(byte)0,(byte)0);
        }

        return headerPos+messageLength();
    }

    private byte header()
    {
        return (byte)0xFD;
    }

    public int messageLength()
    {
        return (byte)0x06;
    }
}
