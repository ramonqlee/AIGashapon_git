package com.idreems.openvm.Push;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ramonqlee on 6/17/16.
 */
public class PushDispatcher {

    private static final int RESERVED_DISPATCHER_ID = -1;
    private static Map<Integer, PushDispatcher> sDispatcherMap = new HashMap<>();

    private ArrayList<PushObserver> mPushObserverList = new ArrayList<>();

    public static PushDispatcher sharedInstance() {
        return sharedInstance(RESERVED_DISPATCHER_ID);
    }

    /**
     * @param dispatcherUID: dispatcherUID>0
     * @return
     */
    public static PushDispatcher sharedInstance(int dispatcherUID) {
        PushDispatcher dispatcher = sDispatcherMap.get(dispatcherUID);
        if (null == dispatcher) {
            dispatcher = new PushDispatcher();
            sDispatcherMap.put(dispatcherUID, dispatcher);
        }
        return dispatcher;
    }

    private PushDispatcher() {
    }

    public void addObserver(PushObserver observer) {
        if (null == observer || -1 != mPushObserverList.indexOf(observer)) {
            return;
        }
        mPushObserverList.add(observer);
    }

    public void removeObserver(PushObserver observer) {
        if (null == observer) {
            return;
        }
        mPushObserverList.remove(observer);
    }

    public void clearObserver() {
        mPushObserverList.clear();
    }

    public void dispatch(String message) {
        for (PushObserver observer : mPushObserverList) {
            if (null == observer) {
                continue;
            }
            observer.onMessage(message);
        }
    }
}
