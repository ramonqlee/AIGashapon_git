package android_serialport_api.vmc.AIGashaponMachine.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ramonqlee on 7/5/16.
 */
public class DataDispatcher {
    public interface Dispatcher {

        String name();

        int handle(byte[] stream);
    }

    private final static DataDispatcher sCPDataDispatcher = new DataDispatcher();
    private List<Dispatcher> mRegList = new ArrayList<Dispatcher>();

    public static DataDispatcher sharedInstance() {
        return sCPDataDispatcher;
    }

    // 数据流分发
    public Dispatcher dispatch(byte[] stream) {
        for (Dispatcher reg : mRegList) {
            if (null == reg) {
                continue;
            }
            if (reg.handle(stream) >= 0) {
                return reg;
            }
        }
        return null;
    }

    public void addRecognizer(Dispatcher reg) {
        if (null == reg) {
            return;
        }
        if (-1 != mRegList.indexOf(reg)) {
            return;
        }
        mRegList.add(reg);
    }

    public List<Dispatcher> getRegList() {
        return mRegList;
    }

    public void removeRecognizer(Dispatcher reg) {
        if (null == reg) {
            return;
        }
        mRegList.remove(reg);
    }

    public void clear() {
        mRegList.clear();
    }
}
