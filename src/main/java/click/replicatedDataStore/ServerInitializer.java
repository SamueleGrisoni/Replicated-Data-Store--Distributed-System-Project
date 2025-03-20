package click.replicatedDataStore;

import click.replicatedDataStore.applicationLayer.Server;
import click.replicatedDataStore.dataStructures.Pair;
import click.replicatedDataStore.dataStructures.ServerPorts;
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
        Map<Integer, Pair<String, ServerPorts>> addresses = readJson(args[0]);
        if(!isMapCorrect(addresses)){
            return;
        }
        System.out.println("Found a config file for " + addresses.size() + " servers");
        System.out.println("--------------------");
        for(Map.Entry<Integer, Pair<String, ServerPorts>> entry : addresses.entrySet()){
            System.out.println("Server " + entry.getKey() + " at " + entry.getValue().first());
            System.out.println("Server port: " + entry.getValue().second().serverPort());
            System.out.println("Client port: " + entry.getValue().second().clientPort());
        }
        System.out.println("--------------------");
        List<Server> servers = new ArrayList<>();
        for(Map.Entry<Integer, Pair<String, ServerPorts>> entry : addresses.entrySet()){
            Server server = new Server(entry.getKey(), addresses);
            server.start();
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
        System.out.println("Servers stopped successfully");
        System.exit(0);
    }

    private static Map<Integer, Pair<String, ServerPorts>> readJson(String filePath) {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<Integer, Pair<String, ServerPorts>> resultMap = new HashMap<>();
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
            int serverIndex = 0;
            for (ConfigFile.ConfigFileEntry entry : configFile.getServers()) {
                resultMap.put(serverIndex, new Pair<>(entry.getIp(), new ServerPorts(entry.getServerPort(), entry.getClientPort())));
                serverIndex++;
            }
        } catch (Exception e) {
            System.out.println("An error occurred while reading the config file: " + e.getMessage());
        }
        return resultMap;
    }

    private static boolean isMapCorrect(Map<Integer, Pair<String, ServerPorts>> addresses){
        Set<Integer> ports = new HashSet<>();
        if(addresses.isEmpty()){
            System.out.println("No servers found in the config file, check the config file syntax");
            return false;
        }
        for(Map.Entry<Integer, Pair<String, ServerPorts>> entry : addresses.entrySet()){
            if(entry.getValue().first() == null || entry.getValue().second() == null){
                System.out.println("Server " + entry.getKey() + " has missing address or port");
                return false;
            }
            if(entry.getValue().second().serverPort() < 1024 || entry.getValue().second().clientPort() < 1024){
                System.out.println("Server " + entry.getKey() + " is using a reserved port. Please use a port number greater than 1023");
                return false;
            }
            if(!ports.add(entry.getValue().second().serverPort())){
                System.out.println("Incoming port " + entry.getValue().second().serverPort() + " of server " + entry.getKey() + " is already in use by another server");
                return false;
            }
            if(!ports.add(entry.getValue().second().clientPort())){
                System.out.println("Outgoing port " + entry.getValue().second().clientPort() + " of server " + entry.getKey() + " is already in use by another server");
                return false;
            }

        }
        return true;
    }
}
