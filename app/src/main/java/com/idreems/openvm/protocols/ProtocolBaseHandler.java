package com.idreems.openvm.protocols;

/**
 * Created by ramonqlee on 5/14/16.
 */
public interface ProtocolBaseHandler {
    public String name();
    public boolean handle(Object object);
}
