package click.replicatedDataStore.utlis;

import java.io.File;

public class ServerConfig {
    public static final String GLOBAL_FOLDER_NAME = "ReplicatedDataStore";
    public static final String SERVER_DATA_FOLDER_NAME = "ReplicatedDataStore-Data";
    public static final String PRIMARY_INDEX_FILE_NAME = "Data-Server-";
    public static final String SECONDARY_INDEX_FILE_NAME = "Data-Secondary-Index-Server-";
    public static final String CONFIG_FILE_HASH_NAME = "Config-File-Hash";
    public static final String FILES_EXTENSION = ".bin";

    public static final int LIGHT_PUSH_DELAY_MILLIS = 1000;
    public static final int LIGHT_PUSH_RANDOM_DELAY_MILLIS = 1000;

    public static int NUMBER_OF_WRITE_BETWEEN_SECONDARY_INDEX_UPDATE = 5;
    public static final boolean debug = true;
    public static final int retryToOpenServerSocketMilliseconds = 500;

   public static String getGlobalFolderPath() {
        String OS = (System.getProperty("os.name")).toUpperCase();
        if (OS.contains("WIN")) {
            return System.getenv("APPDATA") + File.separator + GLOBAL_FOLDER_NAME + File.separator;
        } else if (OS.contains("MAC")) {
            //well-known hardcoded paths, used in the appDirs library
            return System.getProperty("user.home") + "/Library/Application Support/" + GLOBAL_FOLDER_NAME + File.separator;
        } else {
            //In a Unix system, data will be saved in the home directory as a hidden folder
            return System.getProperty("user.home") + File.separator + "." + GLOBAL_FOLDER_NAME + File.separator;
        }
   }


}
