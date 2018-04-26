package com.idreems.openvm.network;

/**
 * Created by ramonqlee on 8/3/16.
 */
public class SignalStrengthUtil {
    /**
     * @hide
     */
    private static final int SIGNAL_STRENGTH_NONE_OR_UNKNOWN = 0;
    /**
     * @hide
     */
    private static final int SIGNAL_STRENGTH_POOR = 1;
    /**
     * @hide
     */
    private static final int SIGNAL_STRENGTH_MODERATE = 2;
    /**
     * @hide
     */
    private static final int SIGNAL_STRENGTH_GOOD = 3;
    /**
     * @hide
     */
    private static final int SIGNAL_STRENGTH_GREAT = 4;

    public static int getLevel(int asu) {
        int level;

        // ASU ranges from 0 to 31 - TS 27.007 Sec 8.5
        // asu = 0 (-113dB or less) is very weak
        // signal, its better to show 0 bars to the user in such cases.
        // asu = 99 is a special case, where the signal strength is unknown.
        if (asu <= 2 || asu == 99) level = SIGNAL_STRENGTH_NONE_OR_UNKNOWN;
        else if (asu >= 12) level = SIGNAL_STRENGTH_GREAT;
        else if (asu >= 8) level = SIGNAL_STRENGTH_GOOD;
        else if (asu >= 5) level = SIGNAL_STRENGTH_MODERATE;
        else level = SIGNAL_STRENGTH_POOR;
        return level;
    }
}
