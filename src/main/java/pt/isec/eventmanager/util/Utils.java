package pt.isec.eventmanager.util;

public class Utils {
    public static String getLabelStyle(LabelType type) {
        switch (type) {
            case ERROR -> {
                return "-fx-text-fill: red";
            }
            case INFO -> {
                return "-fx-text-fill: #00CDCD";
            }
            default -> {
                return "-fx-text-fill: #00CDCD";
            }
        }
    }
}
