package click.replicatedDataStore.applicationLayer;

import click.replicatedDataStore.applicationLayer.serverComponents.Logger;
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
    private final String serverName;
    private final int serverIndex;
    private final int serverNumber;
    private final ServerDataSynchronizer serverDataSynchronizer;
    private final DataManagerWriter dataManagerWriter;
    private final ServerConnectionManager serverConnectionManager;
    private final ClientConnectionManager clientConnectionManager;
    private final TimeTravel timeTravel;
    private volatile boolean stop = false;
    private final Logger logger = new Logger(this);

    public Server(String serverName, int serverIndex, Map<Integer, Pair<String, ServerPorts>> addresses, Boolean isPersistent) {
        this.serverName = serverName;
        this.serverIndex = serverIndex;
        this.addresses = addresses;
        this.serverNumber = addresses.size();

        this.serverDataSynchronizer = new ServerDataSynchronizer(serverName, serverNumber, serverIndex, isPersistent);
        this.dataManagerWriter = new DataManagerWriter(serverDataSynchronizer);
        DataManagerReader dataManagerReader = new DataManagerReader(serverDataSynchronizer);

        this.timeTravel = new TimeTravel(serverDataSynchronizer, dataManagerReader, dataManagerWriter);
        dataManagerWriter.setTimeTravel(timeTravel);

        this.serverConnectionManager = new ServerConnectionManager(timeTravel, logger, this);
        this.timeTravel.setServerConnectionManager(serverConnectionManager);

        this.clientConnectionManager = new ClientConnectionManager(addresses.get(serverIndex).second().clientPort(),
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
        return addresses.get(serverIndex);
    }

    public Pair<String, ServerPorts> getAddressAndPortsPairOf(int serverIndex){
        return addresses.get(serverIndex);
    }

    public int getNumberOfServers(){
        return serverNumber;
    }

    public int getServerIndex(){
        return serverIndex;
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
        logger.logInfo("Server " + serverName +" (server index " + serverIndex + ") started on " + addresses.get(serverIndex).first() + ":" + addresses.get(serverIndex).second());
        logger.logInfo("Server on Thread " + Thread.currentThread().getName() );
        while(!stop){
        }
        stopThreads();
        logger.logInfo("Server " + serverName +" (server index " + serverIndex + ") stopped");
    }

    public void stopServer(){
        stop = true;
    }

    public String getServerName(){
        return serverName;
    }
}