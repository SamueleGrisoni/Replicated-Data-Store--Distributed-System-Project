package click.replicatedDataStore.applicationLayer;

import click.replicatedDataStore.applicationLayer.serverComponents.ServerDataSynchronizer;
import click.replicatedDataStore.applicationLayer.serverComponents.dataManager.DataManagerReader;
import click.replicatedDataStore.applicationLayer.serverComponents.dataManager.DataManagerWriter;
import click.replicatedDataStore.applicationLayer.serverComponents.TimeTravel;
import click.replicatedDataStore.connectionLayer.connectionManagers.ClientConnectionManager;
import click.replicatedDataStore.connectionLayer.connectionManagers.ServerConnectionManager;
import click.replicatedDataStore.dataStructures.ClientWrite;
import click.replicatedDataStore.dataStructures.ClockedData;
import click.replicatedDataStore.dataStructures.Pair;
import click.replicatedDataStore.dataStructures.ServerPorts;

import java.util.*;

public class Server extends Thread{
    private final Map<Integer, Pair<String, ServerPorts>> addresses;
    private final int serverID;
    private final int serverNumber;

    private final ServerDataSynchronizer serverDataSynchronizer;
    private final DataManagerReader dataManagerReader;
    private final DataManagerWriter dataManagerWriter;
    private final ServerConnectionManager serverConnectionManager;
    private final ClientConnectionManager clientConnectionManager;
    private final TimeTravel timeTravel;
    private boolean stop = false;
    private final Logger logger = new Logger();

    public Server(int serverID, Map<Integer, Pair<String, ServerPorts>> addresses) {
        this.serverID = serverID;
        this.addresses = addresses;
        this.serverNumber = addresses.size();

        this.serverDataSynchronizer = new ServerDataSynchronizer(serverNumber, serverID);
        this.dataManagerWriter = new DataManagerWriter(serverDataSynchronizer);
        this.dataManagerReader = new DataManagerReader(serverDataSynchronizer);

        this.timeTravel = new TimeTravel(serverDataSynchronizer, dataManagerReader, dataManagerWriter);
        dataManagerWriter.setTimeTravel(timeTravel);

        this.serverConnectionManager = new ServerConnectionManager(timeTravel, logger, this);
        this.timeTravel.setServerConnectionManager(serverConnectionManager);

        this.clientConnectionManager = new ClientConnectionManager(addresses.get(serverID).second().clientPort(),
                                            dataManagerWriter.getQueue(), dataManagerReader, logger);
    }

    private void startServerThreads(){
        dataManagerWriter.start();
    }
    private void stopThreads() {
        dataManagerWriter.stopThread();
        serverConnectionManager.stop();
        clientConnectionManager.stop();
        timeTravel.stop();
    }

    public Pair<String, ServerPorts> getMyAddressAndPorts(){
        return addresses.get(serverID);
    }

    public Pair<String, ServerPorts> getAddressAndPortsPairOf(int serverID){
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

    @Override
    public void run() {
        startServerThreads();
        logger.logInfo("Server " + serverID + " started on " + addresses.get(serverID).first() + ":" + addresses.get(serverID).second());
        while(!stop){
        }
        stopThreads();
    }

    public void stopServer(){
        stop = true;
    }
}