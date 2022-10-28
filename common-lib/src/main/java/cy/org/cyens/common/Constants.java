package cy.org.cyens.common;

/**
 * Defines several constants used between {@link BluetoothService}.
 */
public interface Constants {

    // Message types sent from the BluetoothService Handler
    int MESSAGE_STATE_CHANGE = 1;
    int MESSAGE_READ = 2;
    int MESSAGE_WRITE = 3;
    int MESSAGE_DEVICE_NAME = 4;
    int MESSAGE_TOAST = 5;

    // Type of commands sent by the remote controller
    enum COMMANDS {
        GET_STATUS,
        RESET_CAMERA_POSE,
        SET_FREQ,
        START,
        STOP,
        PLAY_SOUND,
        CAMERA_DISPLAY,
        RAISE_VOLUME,
        LOWER_VOLUME,
        SET_MUSICIANS,
        SET_MAX_VALUE,
        SET_MIN_VALUE,
        SET_WEIGHT,
        SET_BASE_IMAGE,
        IMAGE,
        RESET_LOG;

        public static COMMANDS fromString(String x) {
            switch(x) {
                case "GET_STATUS":
                    return GET_STATUS;
                case "RESET_CAMERA_POSE":
                    return RESET_CAMERA_POSE;
                case "SET_FREQ":
                    return SET_FREQ;
                case "START":
                    return START;
                case "STOP":
                    return STOP;
                case "PLAY_SOUND":
                    return PLAY_SOUND;
                case "CAMERA_DISPLAY":
                    return CAMERA_DISPLAY;
                case "RAISE_VOLUME":
                    return RAISE_VOLUME;
                case "LOWER_VOLUME":
                    return LOWER_VOLUME;
                case "SET_MUSICIANS":
                    return SET_MUSICIANS;
                case "SET_MAX_VALUE":
                    return SET_MAX_VALUE;
                case "SET_MIN_VALUE":
                    return SET_MIN_VALUE;
                case "SET_WEIGHT":
                    return SET_WEIGHT;
                case "SET_BASE_IMAGE":
                    return SET_BASE_IMAGE;
                case "IMAGE":
                    return IMAGE;
                case "RESET_LOG":
                    return RESET_LOG;
            }
            return null;
        }
    }

    // Key names received from the BluetoothService Handler
    String DEVICE_NAME = "device_name";
    String TOAST = "toast";
}
