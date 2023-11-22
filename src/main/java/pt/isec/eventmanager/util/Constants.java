package pt.isec.eventmanager.util;

import java.io.Serial;
import java.io.Serializable;

public class Constants implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public static final int SERVER_TIMEOUT = 10;

    public static final int HEARTBEAT_BACKUP_TIMEOUT = 30;
    public static final int HEARTBEAT_MAX_SIZE = 10000;

    public static final String DB_FILE_NAME = "eventmanagerdb.sqlite";

    //BACKUP
    public static final int HEARTBEAT_PORT = 4444;
    public static final String HEARTBEAT_URL = "230.44.44.44";

    //EMAIL
    public static final String ADMIN_EMAIL = "PD2023TPEM@gmail.com";
    public static final String ADMIN_EMAIL_PASSWORD = "12345Admin!";

    //USERS
    public static final String AUTHENTICATION_REQUEST = "AUTHENTICATE";
    public static final String INSERTUSER_REQUEST = "INSERTUSER";
    public static final String EDITUSER_REQUEST = "EDITUSER";
    public static final String LISTUSEREVENTS_REQUEST = "LISTUSEREVENTS";

    //EVENTS
    public static final String LISTEVENTS_REQUEST = "LISTEVENTS";
    public static final String INSERTEVENT_REQUEST = "INSERTEVENT";
    public static final String DELETEEVENT_REQUEST = "DELETEEVENT";
    public static final String EDITEVENT_REQUEST = "EDITEVENT";
    public static final String EVENTHASATTENDENCES_REQUEST = "EVENTHASATTENDENCES";
    public static final String LISTATTENDENCES_REQUEST = "LISTATTENDENCES";
    public static final String ADDATTENDENCE_REQUEST = "ADDATTENDENCE";
    public static final String DELETEATTENDENCE_REQUEST = "DELETEATTENDENCE";

    //EVENT KEY
    public static final String GENERATEEVENTKEY_REQUEST = "GENERATEEVENTKEY";
    public static final String GETEVENTKEY_REQUEST = "GETEVENTKEY";

    //USER KEY
    public static final String INSERTUSERKEY_REQUEST = "INSERTUSERKEY";
}
