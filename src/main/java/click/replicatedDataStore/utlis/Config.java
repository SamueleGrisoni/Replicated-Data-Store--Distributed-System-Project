package click.replicatedDataStore.utlis;

import click.replicatedDataStore.dataStructures.Pair;

import java.util.Map;

public class Config {
    public static Map<Integer, Pair<String, String>> addresses = Map.of(
            0, new Pair<>("127.0.0.1", "4416")
    );

    public static void createServer(int serverNumber){
        if(serverNumber > 1) {
            for(int i = 1; i < serverNumber; i++) {
                addresses.put(i, new Pair<>("127.0.0.1", String.valueOf(4416 + i)));
            }
        }
    }

    public static Pair<String, String> getServerAddress(int serverID){
        return addresses.get(serverID);
    }

    public static final String DATA_FOLDER_NAME = "ReplicatedDataStore-Data";
    public static final String PRIMARY_INDEX_FILE_NAME = "Data-Server-";
    public static final String SECONDARY_INDEX_FILE_NAME = "Data-Secondary-Index-Server-";
    public static final String FILES_EXTENSION = ".bin";
}
