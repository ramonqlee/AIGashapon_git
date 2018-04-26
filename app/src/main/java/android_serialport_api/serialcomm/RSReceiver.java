package android_serialport_api.serialcomm;

/**
 * Created by ramonqlee on 6/24/16.
 */
public interface RSReceiver {
    public void onReceive(byte[] data, int size);
}
