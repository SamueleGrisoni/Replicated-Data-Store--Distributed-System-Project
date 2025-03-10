package click.replicatedDataStore.utlis;

import click.replicatedDataStore.dataStructures.Pair;

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
}
