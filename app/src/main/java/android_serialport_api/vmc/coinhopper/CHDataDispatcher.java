package android_serialport_api.vmc.coinhopper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ramonqlee on 7/5/16.
 */
public class CHDataDispatcher {
    public static final int ILLEGAL_HEADER = -1;

    public interface Recoginizer {

        public String name();

        public int handle(byte[] stream);

        public int messageLength();
    }

    private final static CHDataDispatcher sCPDataDispatcher = new CHDataDispatcher();
    private List<Recoginizer> mRegList = new ArrayList<Recoginizer>();

    public static CHDataDispatcher sharedInstance() {
        return sCPDataDispatcher;
    }

    // 数据流分发
    public Recoginizer dispatch(byte[] stream) {
        for (Recoginizer reg : mRegList) {
            if (null == reg) {
                continue;
            }
            if (reg.handle(stream)>0) {
                return reg;
            }
        }
        return null;
    }

    public void addRecognizer(Recoginizer reg) {
        if (null == reg) {
            return;
        }
        if (-1 != mRegList.indexOf(reg)) {
            return;
        }
        mRegList.add(reg);
    }

    public void removeRecognizer(Recoginizer reg) {
        if (null == reg) {
            return;
        }
        mRegList.remove(reg);
    }
}
