package click.replicatedDataStore;

import click.replicatedDataStore.applicationLayer.Logger;
import click.replicatedDataStore.applicationLayer.Server;
import click.replicatedDataStore.dataStructures.Pair;
import click.replicatedDataStore.utlis.ConfigFile;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ServerInitializer {
    public static void main(String[] args) {
        if(args.length != 1){
            System.out.println("Usage: java -jar Server.jar configFilePath");
            return;
        }
        Map<Integer, Pair<String, Integer>> addresses = readJson(args[0]);
        if(addresses.isEmpty()){
            return;
        }
        System.out.println("Found a config file for " + addresses.size() + " servers");
        List<Server> servers = new ArrayList<>();
        for(Map.Entry<Integer, Pair<String, Integer>> entry : addresses.entrySet()){
            Server server = new Server(entry.getKey(), addresses);
            server.run();
            servers.add(server);
        }
        System.out.println("Successfully created " + servers.size() + " servers");
        Scanner scanner = new Scanner(System.in);
        while(true){
            System.out.println("Enter 'exit' to stop the servers");
            String input = scanner.nextLine();
            if(input.equals("exit")){
                for(Server server : servers){
                    server.stopServer();
                }
                break;
            }
        }
    }

    private static Map<Integer, Pair<String, Integer>> readJson(String filePath) {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<Integer, Pair<String, Integer>> resultMap = new HashMap<>();
        ConfigFile configFile;
        try {
            try{
                 configFile = objectMapper.readValue(new File(filePath), ConfigFile.class);
            }catch(IOException e){
                System.out.println("Config file not found at " + filePath);
                System.out.println("Usage: java -jar Server.jar configFilePath");
                return resultMap;
            }
            if(configFile.getServers() == null){
                System.out.println("No servers found in the config file");
                return resultMap;
            }
            if(configFile.getServers().size() == 1){
                System.out.println("To correctly run the system, at least 2 servers are required");
                return resultMap;
            }
            for (ConfigFile.ConfigFileEntry entry : configFile.getServers()) {
                resultMap.put(entry.getServerIndex(), new Pair<>(entry.getIp(), entry.getPort()));
            }
        } catch (Exception e) {
            System.out.println("An error occurred while reading the config file: " + e.getMessage());
        }
        return resultMap;
    }


}
