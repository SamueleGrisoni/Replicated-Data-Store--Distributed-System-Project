package click.replicatedDataStore.applicationLayer;

import click.replicatedDataStore.applicationLayer.serverComponents.ServerDataSynchronizer;
import click.replicatedDataStore.applicationLayer.serverComponents.dataManager.DataManagerReader;
import click.replicatedDataStore.applicationLayer.serverComponents.dataManager.DataManagerWriter;
import click.replicatedDataStore.applicationLayer.serverComponents.Persist;
import click.replicatedDataStore.applicationLayer.serverComponents.TimeTravel;
import click.replicatedDataStore.connectionLayer.connectionManagers.ServerConnectionManager;
import click.replicatedDataStore.dataStructures.ClientWrite;
import click.replicatedDataStore.dataStructures.ClockedData;
import click.replicatedDataStore.dataStructures.VectorClock;
import click.replicatedDataStore.utlis.ServerConfig;
import click.replicatedDataStore.utlis.Key;
import click.replicatedDataStore.dataStructures.Pair;

import java.io.Serializable;
import java.util.*;

public class Server {
    private final Map<Integer, Pair<String, Integer>> addresses;
    private final int serverID;
    private final int serverNumber;

    private final ServerDataSynchronizer serverDataSynchronizer;
    private final DataManagerReader dataManagerReader;
    private final DataManagerWriter dataManagerWriter;
    private final ServerConnectionManager serverConnectionManager;
    private final TimeTravel timeTravel;

    private final Logger logger = new Logger();

    public Server(int serverID, Map<Integer, Pair<String, Integer>> addresses) {
        this.serverID = serverID;
        this.addresses = addresses;
        this.serverNumber = addresses.size();
        String serverAddress = addresses.get(serverID).first();
        Integer serverPort = addresses.get(serverID).second();

        this.serverDataSynchronizer = new ServerDataSynchronizer(serverNumber, serverID);
        this.dataManagerWriter = new DataManagerWriter(serverDataSynchronizer);
        this.dataManagerReader = new DataManagerReader(serverDataSynchronizer);

        this.timeTravel = new TimeTravel(serverDataSynchronizer, dataManagerReader, dataManagerWriter);
        dataManagerWriter.setTimeTravel(timeTravel);

        this.serverConnectionManager = new ServerConnectionManager(timeTravel, logger, this);
        this.timeTravel.setServerConnectionManager(serverConnectionManager);

        startServerThreads();
        logger.logInfo("Server " + serverID + " started on " + serverAddress + ":" + serverPort);
    }

    private void startServerThreads(){
        dataManagerWriter.start();
    }
    public void stopThreads() {
        dataManagerWriter.stopThread();
    }

    public Pair<String, Integer> getMyAddressAndPort(){
        return addresses.get(serverID);
    }

    public Pair<String, Integer> getAddressAndPortPairOf(int serverID){
        return addresses.get(serverID);
    }

    public int getNumberOfServers(){
        return serverNumber;
    }

    public int getServerID(){
        return serverID;
    }

    public Set<Integer> getOtherIndexes(){
        Set<Integer> list = addresses.keySet();
        list.remove(this.serverID);
        return list;
    }

    public void addClientData(ClientWrite clientWrite){
        dataManagerWriter.addClientData(clientWrite);
    }

    public void addServerData(List<ClockedData> serverData){
        dataManagerWriter.addServerData(serverData);
    }
}