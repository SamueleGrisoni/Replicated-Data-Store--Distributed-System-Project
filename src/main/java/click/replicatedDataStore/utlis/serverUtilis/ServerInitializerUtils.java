package click.replicatedDataStore.utlis.serverUtilis;

import click.replicatedDataStore.applicationLayer.Server;
import click.replicatedDataStore.dataStructures.Pair;
import click.replicatedDataStore.dataStructures.ServerPorts;
import click.replicatedDataStore.utlis.configs.ConfigFile;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.util.*;

public class ServerInitializerUtils {
    //Map passed to each Server object, it contains all addresses of all servers. Integer is server index
    private final Map<Integer, Pair<String, ServerPorts>> addresses = new LinkedHashMap<>();
    //Pair of maps used internally to differentiate local and other server. Integer is server index
    private Pair<Map<Integer, Pair<String, ServerPorts>>, Map<Integer, Pair<String, ServerPorts>>> addressesPair;
    private final Map<Integer, Integer> serverIdToIndex = new HashMap<>();
    private static final Map<Integer, Integer> serverIndexToId = new HashMap<>();
    private final Map<Integer, Pair<Server, Boolean>> localServerStatus = new HashMap<>();

    public Map<Integer, Pair<String, ServerPorts>> computeAddress(String filePath) {
        Pair<List<ConfigFile.ConfigFileEntry>, List<ConfigFile.ConfigFileEntry>> addressesListPair = readJson(filePath);
        ServerDataFolderUtils.checkConfigFileHasChanged(addressesListPair);
        fillAddressPairMap(addressesListPair);
        addresses.putAll(addressesPair.first());
        addresses.putAll(addressesPair.second());
        if (!isMapCorrect(addresses)) {
            System.exit(1);
        }
        return addresses;
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

    public void startLocalServer() {
        for (Map.Entry<Integer, Pair<String, ServerPorts>> entry : addressesPair.first().entrySet()) {
            Server server = new Server(entry.getKey(), addresses);
            server.start();
            localServerStatus.put(entry.getKey(), new Pair<>(server, true));
        }
        System.out.println("Successfully started " + localServerStatus.size() + " servers");
    }

    public void closeAllLocalServer() {
        for (Map.Entry<Integer, Pair<Server, Boolean>> entry : localServerStatus.entrySet()) {
            if (entry.getValue().second()) { //If the server is running
                entry.getValue().first().stopServer();
            }
        }
    }

    public void stopOrStartLocalServer(int serverId) {
        Integer serverIndex = serverIdToIndex.get(serverId);
        if (serverIndex == null || !addressesPair.first().containsKey(serverIndex)) {
            System.out.println("Server " + serverId + " is not an id of a local server");
            return;
        }
        Pair<Server, Boolean> serverStatus = localServerStatus.get(serverIndex);
        if (serverStatus.second()) { //server is on
            serverStatus.first().stopServer();
            localServerStatus.put(serverIndex, new Pair<>(serverStatus.first(), false));
            System.out.println("Server " + serverId + " stopped successfully");
        } else {
            try {
                Server restartedServer = new Server(serverIndex, addresses);
                restartedServer.start();
                System.out.println("Server " + serverId + " restarted successfully");
                localServerStatus.put(serverIndex, new Pair<>(restartedServer, true));
            } catch (RuntimeException e) {
                //todo fix
                e.printStackTrace();
                System.out.println("Server " + serverId + " socket port is still in use, try again in a few moment");
            } catch (Exception e) {
                System.out.println("Server " + serverId + " failed to restart");
            }
        }
    }

    public static int getServerIdFromIndex(Integer serverIndex) {
        return serverIndexToId.get(serverIndex);
    }

    private Pair<List<ConfigFile.ConfigFileEntry>, List<ConfigFile.ConfigFileEntry>> readJson(String filePath) {
        ObjectMapper objectMapper = new ObjectMapper();
        ConfigFile configFile = null;
        try {
            try {
                configFile = objectMapper.readValue(new File(filePath), ConfigFile.class);
            } catch (IOException e) {
                System.out.println("Config file not found at " + filePath);
                System.out.println("Usage: java -jar Server.jar configFilePath");
                System.exit(1);
            }
            if (!jsonHealthCheck(configFile)) {
                System.exit(1);
            }
            return new Pair<>(getServerList(configFile, true), getServerList(configFile, false));
        } catch (Exception e) {
            System.out.println("An error occurred while reading the config file: " + e.getMessage());
            System.exit(1);
            return null;
        }
    }

    private boolean jsonHealthCheck(ConfigFile configFile) {
        if (configFile == null) {
            System.out.println("An error occurred while reading the config file");
            return false;
        }
        if (configFile.getLocalServer() == null && configFile.getOtherServers() == null) {
            System.out.println("No servers found in the config file");
            return false;
        }
        if ((configFile.getLocalServer().size() + configFile.getOtherServers().size()) < 2) {
            System.out.println("To correctly run the system, at least 2 servers are required");
            return false;
        }
        return true;
    }

    //Return a List of servers sorted by serverId, lower id first
    private List<ConfigFile.ConfigFileEntry> getServerList(ConfigFile configFile, boolean localServer) {
        List<ConfigFile.ConfigFileEntry> list = new ArrayList<>();
        if (localServer) {
            list.addAll(configFile.getLocalServer());
        } else {
            list.addAll(configFile.getOtherServers());
        }
        list.sort(Comparator.comparing(ConfigFile.ConfigFileEntry::getServerId));
        return list;
    }

    private void fillAddressPairMap(Pair<List<ConfigFile.ConfigFileEntry>, List<ConfigFile.ConfigFileEntry>> addressPair) {
        List<Pair<ConfigFile.ConfigFileEntry, Boolean>> totalListLocal = new ArrayList<>();
        for (ConfigFile.ConfigFileEntry entry : addressPair.first()) {
            totalListLocal.add(new Pair<>(entry, true));
        }
        for (ConfigFile.ConfigFileEntry entry : addressPair.second()) {
            totalListLocal.add(new Pair<>(entry, false));
        }
        totalListLocal.sort(Comparator.comparingInt(o -> o.first().getServerId()));
        Map<Integer, Pair<String, ServerPorts>> localServer = new LinkedHashMap<>();
        Map<Integer, Pair<String, ServerPorts>> otherServers = new LinkedHashMap<>();
        int serverIndex = 0;
        for (Pair<ConfigFile.ConfigFileEntry, Boolean> entry : totalListLocal) {
            if (entry.second()) { //Local server
                serverIdToIndex.put(entry.first().getServerId(), serverIndex);
                serverIndexToId.put(serverIndex, entry.first().getServerId());
                localServer.put(serverIndex, new Pair<>(entry.first().getIp(), new ServerPorts(entry.first().getServerPort(), entry.first().getClientPort())));
            } else {
                serverIdToIndex.put(entry.first().getServerId(), serverIndex);
                serverIndexToId.put(serverIndex, entry.first().getServerId());
                otherServers.put(serverIndex, new Pair<>(entry.first().getIp(), new ServerPorts(entry.first().getServerPort(), entry.first().getClientPort())));
            }
            serverIndex++;
        }
        addressesPair = new Pair<>(localServer, otherServers);
    }

    private boolean isMapCorrect(Map<Integer, Pair<String, ServerPorts>> addresses) {
        Set<Integer> ports = new HashSet<>();
        if (addresses.isEmpty()) {
            System.out.println("No servers found in the config file, check the config file syntax");
            return false;
        }
        for (Map.Entry<Integer, Pair<String, ServerPorts>> entry : addresses.entrySet()) {
            if (entry.getValue().first() == null || entry.getValue().second() == null) {
                System.out.println("Server " + entry.getKey() + " has missing address or port");
                return false;
            }
            if (entry.getValue().second().serverPort() < 1024 || entry.getValue().second().clientPort() < 1024) {
                System.out.println("Server " + entry.getKey() + " is using a reserved port. Please use a port number greater than 1023");
                return false;
            }
            if (!ports.add(entry.getValue().second().serverPort())) {
                System.out.println("Server port " + entry.getValue().second().serverPort() + " of server " + getServerIdFromIndex(entry.getKey()) + " is already in use by another server. Check the config file");
                return false;
            }
            if (!ports.add(entry.getValue().second().clientPort())) {
                System.out.println("Client port " + entry.getValue().second().clientPort() + " of server " + getServerIdFromIndex(entry.getKey()) + " is already in use by another server. Check the config file");
                return false;
            }
        }
        return true;
    }
}
