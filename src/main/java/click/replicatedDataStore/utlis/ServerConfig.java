package click.replicatedDataStore.utlis;

import click.replicatedDataStore.dataStructures.Pair;

import java.io.File;
import java.util.Map;

public class ServerConfig {
    public static final String DATA_FOLDER_NAME = "ReplicatedDataStore-Data";
    public static final String PRIMARY_INDEX_FILE_NAME = "Data-Server-";
    public static final String SECONDARY_INDEX_FILE_NAME = "Data-Secondary-Index-Server-";
    public static final String FILES_EXTENSION = ".bin";

    public static final int LIGHT_PUSH_DELAY_MILLIS = 1000;
    public static final int LIGHT_PUSH_RANDOM_DELAY_MILLIS = 1000;

    public static int NUMBER_OF_WRITE_BETWEEN_SECONDARY_INDEX_UPDATE = 5;
    public static final boolean debug = true;
    public static final int retryToOpenServerSocketMilliseconds = 500;

    /**
     * Get the path of the data folder based on the OS (Windows, macOS, Linux)
     * data folder is the folder where the data is going to be persisted
     *
     * @return the path of the data folder depending on the OS
     */
   public static String getOSDataFolderPath() {
        String OS = (System.getProperty("os.name")).toUpperCase();
        if (OS.contains("WIN")) {
            return System.getenv("APPDATA") + File.separator;
        } else if (OS.contains("MAC")) {
            //well-known hardcoded paths, used in the appDirs library
            return System.getProperty("user.home") + "/Library/Application Support/";
        } else {
            //In a Unix system, data will be saved in the home directory as a hidden folder
            return System.getProperty("user.home") + File.separator + ".";
        }
    }
}
