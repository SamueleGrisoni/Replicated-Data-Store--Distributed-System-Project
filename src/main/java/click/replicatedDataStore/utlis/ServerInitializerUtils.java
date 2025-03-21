package click.replicatedDataStore.utlis;

import click.replicatedDataStore.applicationLayer.Server;
import click.replicatedDataStore.dataStructures.Pair;
import click.replicatedDataStore.dataStructures.ServerPorts;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ServerInitializerUtils {
    private final Map<Integer, Pair<String, ServerPorts>> addresses = new LinkedHashMap<>();
    private Pair<Map<Integer, Pair<String, ServerPorts>>, Map<Integer, Pair<String, ServerPorts>>> addressesPair;
    private final Map<Integer, Integer> serverIdToIndex = new HashMap<>();
    private static final Map<Integer, Integer> serverIndexToId = new HashMap<>();
    private final Map<Integer, Pair<Server, Boolean>> localServerStatus = new HashMap<>();

    public Map<Integer, Pair<String, ServerPorts>> computeAddress(String filePath){
        readJson(filePath);
        addresses.putAll(addressesPair.first());
        addresses.putAll(addressesPair.second());
        if(!isMapCorrect(addresses)){
            System.exit(1);
        }
        return addresses;
    }

    private void readJson(String filePath) {
        Map<Integer, Pair<String, ServerPorts>> localServer = new LinkedHashMap<>();
        Map<Integer, Pair<String, ServerPorts>> otherServers = new LinkedHashMap<>();
        ObjectMapper objectMapper = new ObjectMapper();
        ConfigFile configFile = null;
        try {
            try{
                configFile = objectMapper.readValue(new File(filePath), ConfigFile.class);
            }catch(IOException e){
                System.out.println("Config file not found at " + filePath);
                System.out.println("Usage: java -jar Server.jar configFilePath");
                System.exit(1);
            }
            if(!jsonHealthCheck(configFile)){
                System.exit(1);
            }
            int serverIndex = 0;
            for (ConfigFile.ConfigFileEntry entry : getServerList(configFile, true)) {
                serverIdToIndex.put(entry.getServerId(), serverIndex);
                serverIndexToId.put(serverIndex, entry.getServerId());
                localServer.put(serverIndex, new Pair<>(entry.getIp(), new ServerPorts(entry.getServerPort(), entry.getClientPort())));
                serverIndex++;
            }
            for (ConfigFile.ConfigFileEntry entry : getServerList(configFile, false)) {
                serverIdToIndex.put(entry.getServerId(), serverIndex);
                serverIndexToId.put(serverIndex, entry.getServerId());
                otherServers.put(serverIndex, new Pair<>(entry.getIp(), new ServerPorts(entry.getServerPort(), entry.getClientPort())));
                serverIndex++;
            }
        } catch (Exception e) {
            System.out.println("An error occurred while reading the config file: " + e.getMessage());
            System.exit(1);
        }
        addressesPair = new Pair<>(localServer, otherServers);
    }

    //Return a queue of servers sorted by serverId, lower id first
    private PriorityQueue<ConfigFile.ConfigFileEntry> getServerList(ConfigFile configFile, boolean localServer){
        PriorityQueue<ConfigFile.ConfigFileEntry> queue = new PriorityQueue<>(
                Comparator.comparingInt(ConfigFile.ConfigFileEntry::getServerId)
        );
        if(localServer){
            queue.addAll(configFile.getLocalServer());
        }else{
            queue.addAll(configFile.getOtherServers());
        }
        return queue;
    }

    private boolean jsonHealthCheck(ConfigFile configFile){
        if(configFile == null){
            System.out.println("An error occurred while reading the config file");
            return false;
        }
        if(configFile.getLocalServer() == null && configFile.getOtherServers() == null){
            System.out.println("No servers found in the config file");
            return false;
        }
        if((configFile.getLocalServer().size() + configFile.getOtherServers().size()) < 2){
            System.out.println("To correctly run the system, at least 2 servers are required");
            return false;
        }
        return true;
    }

    private boolean isMapCorrect(Map<Integer, Pair<String, ServerPorts>> addresses){
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
                System.out.println("Server port " + entry.getValue().second().serverPort() + " of server " + entry.getKey() + " is already in use by another server");
                return false;
            }
            if(!ports.add(entry.getValue().second().clientPort())){
                System.out.println("Client port " + entry.getValue().second().clientPort() + " of server " + entry.getKey() + " is already in use by another server");
                return false;
            }
        }
        return true;
    }

    public void printServerList() {
        final String RESET = "\u001B[0m";
        final String LOCAL_COLOR = "\u001B[32m"; // Green
        final String OTHER_COLOR = "\u001B[34m"; // Blue

        if (!addressesPair.first().isEmpty()) {
            System.out.println(LOCAL_COLOR + "---------LOCAL SERVERS-----------" + RESET);
            for (Map.Entry<Integer, Pair<String, ServerPorts>> entry : addressesPair.first().entrySet()) {
                System.out.printf("Server %d at %s (Server port: %d, Client port: %d)%n",
                        getServerIdFromIndex(entry.getKey()), entry.getValue().first(), entry.getValue().second().serverPort(), entry.getValue().second().clientPort());
            }
            System.out.println(LOCAL_COLOR + "-------------------------------" + RESET);
        }
        if (!addressesPair.second().isEmpty()) {
            System.out.println(OTHER_COLOR + "---------OTHER SERVERS-----------" + RESET);
            for (Map.Entry<Integer, Pair<String, ServerPorts>> entry : addressesPair.second().entrySet()) {
                System.out.printf("Server %d at %s (Server port: %d, Client port: %d)%n",
                        getServerIdFromIndex(entry.getKey()), entry.getValue().first(), entry.getValue().second().serverPort(), entry.getValue().second().clientPort());
            }
            System.out.println(OTHER_COLOR + "-------------------------------" + RESET);
        }
    }

    public void startLocalServer(){
        for(Map.Entry<Integer, Pair<String, ServerPorts>> entry : addressesPair.first().entrySet()){
            Server server = new Server(entry.getKey(), addresses);
            server.start();
            localServerStatus.put(entry.getKey(), new Pair<>(server, true));
        }
        System.out.println("Successfully started " + localServerStatus.size() + " servers");
    }

    public void closeAllLocalServer(){
        for(Map.Entry<Integer, Pair<Server, Boolean>> entry : localServerStatus.entrySet()){
            if(entry.getValue().second()){ //If the server is running
                entry.getValue().first().stopServer();
            }
        }
    }

    public void stopOrStartLocalServer(int serverId){
        Integer serverIndex = serverIdToIndex.get(serverId);
        if(serverIndex == null || !addressesPair.first().containsKey(serverIndex)){
            System.out.println("Server " + serverId + " is not an id of a local server");
            return;
        }
        Pair<Server, Boolean> serverStatus = localServerStatus.get(serverIndex);
        if(serverStatus.second()){ //server is on
            serverStatus.first().stopServer();
            localServerStatus.put(serverIndex, new Pair<>(serverStatus.first(), false));
            System.out.println("Server " + serverId + " stopped successfully");
        }else{
            try{
                Server restartedServer = new Server(serverIndex, addresses);
                restartedServer.start();
                System.out.println("Server " + serverId + " restarted successfully");
                localServerStatus.put(serverIndex, new Pair<>(restartedServer, true));
            }catch (RuntimeException e){
                //todo fix
                e.printStackTrace();
                System.out.println("Server " + serverId + " socket port is still in use, try again in a few moment");
            }catch (Exception e){
                System.out.println("Server " + serverId + " failed to restart");
            }
        }
    }

    public static int getServerIdFromIndex(Integer serverIndex){
        return serverIndexToId.get(serverIndex);
    }
}
