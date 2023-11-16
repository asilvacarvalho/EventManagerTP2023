module pt.isec.eventmanager {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    exports pt.isec.eventmanager;
    exports pt.isec.eventmanager.events;
    exports pt.isec.eventmanager.users;
    exports pt.isec.eventmanager.server;
    exports pt.isec.eventmanager.client;
    exports pt.isec.eventmanager.util;

    opens pt.isec.eventmanager to javafx.fxml;
    opens pt.isec.eventmanager.events to javafx.fxml;
    opens pt.isec.eventmanager.users to javafx.fxml;
    opens pt.isec.eventmanager.server to javafx.fxml;
    opens pt.isec.eventmanager.client to javafx.fxml;
    opens pt.isec.eventmanager.util to javafx.fxml;
}