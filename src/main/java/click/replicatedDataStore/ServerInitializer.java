package click.replicatedDataStore;

import click.replicatedDataStore.applicationLayer.Logger;
import click.replicatedDataStore.applicationLayer.Server;
import click.replicatedDataStore.dataStructures.Pair;
import click.replicatedDataStore.utlis.ConfigFile;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
/**
 * Hello world!
 */
public class ServerInitializer {
    public static void main(String[] args) {
        Logger logger = new Logger();
        logger.logInfo("Hello World!");
        if(args.length != 1){
            logger.logErr(ServerInitializer.class, "Usage: java -jar Server.jar configFilePath");
            return;
        }
        Map<Integer, Pair<String, Integer>> addresses = readJson(args[0], logger);
        Integer serverIndex = readServerIndex(args[0], logger);
        if(addresses.isEmpty() || serverIndex == null){
            logger.logErr(ServerInitializer.class, "Error reading config file");
            return;
        }
        new Server(serverIndex, addresses);
    }

    private static Map<Integer, Pair<String, Integer>> readJson(String filePath, Logger logger) {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<Integer, Pair<String, Integer>> resultMap = new HashMap<>();
        try{
            ConfigFile configFile = objectMapper.readValue(new File(filePath), ConfigFile.class);
            for(Map.Entry<Integer, ConfigFile.ConfigFileEntry> entry : configFile.getAddresses().entrySet()){
                resultMap.put(entry.getKey(), new Pair<>(entry.getValue().getIp(), entry.getValue().getPort()));
            }
        } catch (Exception e){
            logger.logErr(ServerInitializer.class, "Error reading config file: " + e.getMessage());
        }
        return resultMap;
    }

    private static Integer readServerIndex(String filePath, Logger logger){
        ObjectMapper objectMapper = new ObjectMapper();
        try{
            JsonNode node = objectMapper.readTree(new FileInputStream(filePath));
            return node.get("serverIndex").asInt();
        } catch (Exception e){
            logger.logErr(ServerInitializer.class, "Error reading server index: " + e.getMessage());
        }
        return null;
    }
}
