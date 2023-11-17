package pt.isec.eventmanager.util;

import java.io.Serial;
import java.io.Serializable;

public class Constants implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public static final int TIMEOUT = 10;
    public static final String AUTHENTICATION_REQUEST = "AUTHENTICATE";
    public static final String INSERTUSER_REQUEST = "INSERTUSER";
    public static final String INSERTEVENT_REQUEST = "INSERTEVENT";
    public static final String LISTEVENTS_REQUEST = "LISTEVENTS";
    public static final String EDITEVENT_REQUEST = "EDITEVENT";
    public static final String DELETEEVENT_REQUEST = "DELETEEVENT";
    public static final String GENERATEKEYEVENT_REQUEST = "GENERATEKEYEVENT";
    public static final String EVENTHASATTENDENCES_REQUEST = "EVENTHASATTENDENCES";
}
