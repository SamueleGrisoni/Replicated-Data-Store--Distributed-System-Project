package click.replicatedDataStore.utils.serverUtilis;

import click.replicatedDataStore.applicationLayer.Server;
import click.replicatedDataStore.dataStructures.Pair;
import click.replicatedDataStore.dataStructures.ServerPorts;
import click.replicatedDataStore.utils.configs.ConfigFile;
import click.replicatedDataStore.utils.configs.LoadedConfig;
import click.replicatedDataStore.utils.configs.LoadedLocalServerConfig;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class ServerInitializerUtils {
    //Map passed to each Server object, it contains all addresses of all servers. Integer is server index
    //A Pair of maps used internally to differentiate local and other server. Integer is server index
    private Pair<Map<Integer, LoadedLocalServerConfig>, Map<Integer, LoadedConfig>> serverConfigs;
    private final Map<String, Integer> serverNameToIndex = new HashMap<>();
    private static final Map<Integer, String> serverIndexToName = new HashMap<>();
    private final Map<Integer, Pair<Server, Boolean>> localServerStatus = new HashMap<>();

    public int loadConfigFilesAndComputeAddress(String addressFilePath) {
        Pair<List<ConfigFile.ConfigFileEntry>, List<ConfigFile.ConfigFileEntry>> addressesListPair = loadAddressConfigFromJson(addressFilePath);
        ServerDataFolderUtils.checkConfigFileHasChanged(addressesListPair);
        loadConfigs(addressesListPair);

        return serverConfigs.first().size() + serverConfigs.second().size();
    }

    public void printServerList() {
        final String RESET = "\u001B[0m";
        final String LOCAL_COLOR = "\u001B[32m"; // Green
        final String OTHER_COLOR = "\u001B[34m"; // Blue

        if (!serverConfigs.first().isEmpty()) {
            System.out.println(LOCAL_COLOR + "---------LOCAL SERVERS-----------" + RESET);
            for (Map.Entry<Integer, LoadedLocalServerConfig> entry : serverConfigs.first().entrySet()) {
                System.out.printf(entry.toString());
            }
            System.out.println(LOCAL_COLOR + "-------------------------------" + RESET);
        }
        if (!serverConfigs.second().isEmpty()) {
            System.out.println(OTHER_COLOR + "---------OTHER SERVERS-----------" + RESET);
            for (Map.Entry<Integer, LoadedConfig> entry : serverConfigs.second().entrySet()) {
                System.out.printf(entry.toString());
            }
            System.out.println(OTHER_COLOR + "-------------------------------" + RESET);
        }
    }

    Map<Integer, Pair<String, ServerPorts>> getAllServersAddresses() {
        Map<Integer, Pair<String, ServerPorts>> addresses = new HashMap<>();
        Map<Integer, LoadedConfig> allConfigs = new HashMap<Integer, LoadedConfig>();
        allConfigs.putAll(serverConfigs.first());
        allConfigs.putAll(serverConfigs.second());

        for(Map.Entry<Integer, LoadedConfig> entry : allConfigs.entrySet()){
            addresses.put(entry.getKey(), new Pair<>(entry.getValue().ip, entry.getValue().ports));
        }

        return addresses;
    }

    public void startAllLocalServer() {
        for (Map.Entry<Integer, LoadedLocalServerConfig> entry : serverConfigs.first().entrySet()) {
            Server server = new Server(serverIndexToName.get(entry.getKey()), entry.getKey(),
                    getAllServersAddresses(), entry.getValue());
            server.start();
            localServerStatus.put(entry.getKey(), new Pair<>(server, true));
        }
        System.out.println("Successfully started " + localServerStatus.size() + " servers");
    }

    public void closeAllLocalServer() {
        System.out.println("Stopping servers...");
        for (Map.Entry<Integer, Pair<Server, Boolean>> entry : localServerStatus.entrySet()) {
            if (entry.getValue().second()) { //If the server is running
                entry.getValue().first().stopServer();
            }
        }
        for (Map.Entry<Integer, Pair<Server, Boolean>> entry : localServerStatus.entrySet()) {
            try {
                entry.getValue().first().join();
            } catch (InterruptedException e) {
                System.out.println("An error occurred while stopping the server: " + serverIndexToName.get(entry.getKey()) + " " + e.getMessage());
            }
        }
        System.out.println("Servers stopped successfully");
    }

    public boolean isLocalServer(String serverName) {
        Integer serverIndex = serverNameToIndex.get(serverName);
        return serverIndex != null && serverConfigs.first().containsKey(serverIndex);
    }

    public boolean isServerRunning(String serverName) {
        Integer serverIndex = serverNameToIndex.get(serverName);
        if (serverIndex == null || !serverConfigs.first().containsKey(serverIndex)) {
            throw new IllegalArgumentException("Server " + serverName + " is not a name of a local server");
        }
        Pair<Server, Boolean> serverStatus = localServerStatus.get(serverIndex);
        return serverStatus.second();
    }

    public void startLocalServer(String serverName) {
        Integer serverIndex = serverNameToIndex.get(serverName);
        try {
            System.out.println("Do you want to enable persistence for server '" + serverName + "'? (y/n)");
            Scanner scanner = new Scanner(System.in);
            boolean isPersistent;
            while(true){
                String input = scanner.nextLine().trim().toLowerCase();
                if (input.equals("y")) {
                    isPersistent = true;
                    break;
                } else if (input.equals("n")) {
                    isPersistent = false;
                    break;
                } else {
                    System.out.println("Invalid input. Please enter 'y' or 'n'.");
                }
            }
            Server restartedServer = new Server(serverName, serverIndex, getAllServersAddresses(), serverConfigs.first().get(serverIndex));
            restartedServer.start();
            System.out.println("Server " + serverName + " restarted successfully with persistence: " + isPersistent);
            localServerStatus.put(serverIndex, new Pair<>(restartedServer, true));
        } catch (RuntimeException e) {
            e.printStackTrace();
            System.out.println("Server " + serverName + " socket port is still in use, try again in a few moment");
        } catch (Exception e) {
            System.out.println("Server " + serverName + " failed to restart");
        }
    }

    public void stopLocalServer(String serverName){
        Integer serverIndex = serverNameToIndex.get(serverName);
        Pair<Server, Boolean> serverStatus = localServerStatus.get(serverIndex);
        serverStatus.first().stopServer();
        try {
            serverStatus.first().join();
        } catch (InterruptedException e) {
            System.out.println("An error occurred while stopping the server: " + serverName + " " + e.getMessage());
        }
        localServerStatus.put(serverIndex, new Pair<>(serverStatus.first(), false));
        System.out.println("Server " + serverName + " stopped successfully");
    }

    private Pair<List<ConfigFile.ConfigFileEntry>, List<ConfigFile.ConfigFileEntry>> loadAddressConfigFromJson(String filePath) {
        ObjectMapper objectMapper = new ObjectMapper();
        ConfigFile configFile = null;
        try {
            try {
                configFile = objectMapper.readValue(new File(filePath), ConfigFile.class);
            } catch (IOException e) {
                System.out.println("address config file not found at " + filePath);
                System.exit(1);
            }
            if (!addressConfigJsonHealthCheck(configFile)) {
                System.exit(1);
            }
            return new Pair<>(getServerList(configFile, true), getServerList(configFile, false));
        } catch (Exception e) {
            System.out.println("An error occurred while reading the config file: " + e.getMessage());
            System.exit(1);
            return null;
        }
    }

    private boolean addressConfigJsonHealthCheck(ConfigFile configFile) {
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
        for (ConfigFile.ConfigFileEntry entry : configFile.getOtherServers()) {
            if (entry.isPersistent() != null) {
                System.out.println("Error: 'isPersistent' is only allowed for local servers, but was found in \"other\" Server: " + entry.getServerName());
                return false;
            }
        }
        for (ConfigFile.ConfigFileEntry entry : configFile.getLocalServer()) {
            if (entry.isPersistent() == null) {
                System.out.println("Error: 'isPersistent' is required for local servers, but was not found in \"local\" Server: " + entry.getServerName());
                return false;
            }
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
        list.sort(Comparator.comparing(ConfigFile.ConfigFileEntry::getServerName));
        return list;
    }

    private void loadConfigs(Pair<List<ConfigFile.ConfigFileEntry>, List<ConfigFile.ConfigFileEntry>> addressPair) {
        List<Pair<ConfigFile.ConfigFileEntry, Boolean>> serverListLocalSigned = new ArrayList<>();
        for (ConfigFile.ConfigFileEntry entry : addressPair.first()) {
            serverListLocalSigned.add(new Pair<>(entry, true));
        }
        for (ConfigFile.ConfigFileEntry entry : addressPair.second()) {
            serverListLocalSigned.add(new Pair<>(entry, false));
        }
        serverListLocalSigned.sort(Comparator.comparing(entry -> entry.first().getServerName()));
        //Map: serverID, Pair<ServerAddress(IP, ServerPorts), isPersistent>
        Map<Integer, LoadedLocalServerConfig> localServer = new LinkedHashMap<>();
        Map<Integer, LoadedConfig> otherServers = new LinkedHashMap<>();
        // construct the indexing of the servers
        int serverIndex = 0;
        for (Pair<ConfigFile.ConfigFileEntry, Boolean> entry : serverListLocalSigned) {
            serverNameToIndex.put(entry.first().getServerName(), serverIndex);
            serverIndexToName.put(serverIndex, entry.first().getServerName());
            serverIndex++;
        }

        for (Pair<ConfigFile.ConfigFileEntry, Boolean> entry : serverListLocalSigned) {
            if (entry.second()) { //Local server
                LoadedLocalServerConfig localConfig = new LoadedLocalServerConfig(
                        entry.first().getServerName(), entry.first().getIp(),
                        new ServerPorts(entry.first().getServerPort(), entry.first().getClientPort()), entry.first().isPersistent(),
                        entry.first().getHeavyPropagationPolicy(), convertServerNamesToIndexes(entry.first().getOverlayNetHeavy()),
                        convertServerNamesToIndexes(entry.first().getOverlayNetLight()));
                localServer.put(serverNameToIndex.get(entry.first().getServerName()), localConfig);

            } else {
                LoadedConfig otherConfig = new LoadedConfig(
                        entry.first().getServerName(), entry.first().getIp(),
                        new ServerPorts(entry.first().getServerPort(), entry.first().getClientPort()));
                otherServers.put(serverNameToIndex.get(entry.first().getServerName()), otherConfig);
            }

        }

        serverConfigs = new Pair<>(localServer, otherServers);
    }

    private Set<Integer> convertServerNamesToIndexes(List<String> serverNames){
        return serverNames.stream().map(this.serverNameToIndex::get).collect(Collectors.toSet());
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
                System.out.println("Server port " + entry.getValue().second().serverPort() + " of server " + serverIndexToName.get(entry.getKey()) + " is already in use by another server. Check the config file");
                return false;
            }
            if (!ports.add(entry.getValue().second().clientPort())) {
                System.out.println("Client port " + entry.getValue().second().clientPort() + " of server " + serverIndexToName.get(entry.getKey()) + " is already in use by another server. Check the config file");
                return false;
            }
        }
        return true;
    }

    public static String getNameFromIndex(int index) {
        return serverIndexToName.get(index);
    }
}
