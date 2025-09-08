package click.replicatedDataStore.applicationLayer;

import click.replicatedDataStore.applicationLayer.serverComponents.Logger;
import click.replicatedDataStore.applicationLayer.serverComponents.ServerDataSynchronizer;
import click.replicatedDataStore.applicationLayer.serverComponents.Synchronizer;
import click.replicatedDataStore.applicationLayer.serverComponents.dataManager.DataManagerReader;
import click.replicatedDataStore.applicationLayer.serverComponents.dataManager.DataManagerWriter;
import click.replicatedDataStore.connectionLayer.connectionManagers.ClientConnectionManager;
import click.replicatedDataStore.connectionLayer.connectionManagers.ServerConnectionManager;
import click.replicatedDataStore.dataStructures.ClientWrite;
import click.replicatedDataStore.dataStructures.ClockedData;
import click.replicatedDataStore.dataStructures.Pair;
import click.replicatedDataStore.dataStructures.ServerPorts;
import click.replicatedDataStore.utils.configs.LoadedLocalServerConfig;

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
    private final Synchronizer synchronizer;
    private volatile boolean stop = false;
    private final Logger logger = new Logger(this);

    public Server(String serverName, int serverIndex,
                  Map<Integer, Pair<String, ServerPorts>> addresses, LoadedLocalServerConfig config) {
        this.serverName = serverName;
        this.serverIndex = serverIndex;
        this.addresses = addresses;
        this.serverNumber = addresses.size();

        this.serverDataSynchronizer = new ServerDataSynchronizer(serverName, serverNumber, serverIndex, config.isPersistent);
        this.dataManagerWriter = new DataManagerWriter(serverDataSynchronizer);
        DataManagerReader dataManagerReader = new DataManagerReader(serverDataSynchronizer);

        this.synchronizer = new Synchronizer(serverDataSynchronizer, dataManagerReader, dataManagerWriter,
                config.heavyPropagationPolicy, config.heavyConnections, config.lightConnections);
        dataManagerWriter.setTimeTravel(synchronizer);

        this.serverConnectionManager = new ServerConnectionManager(synchronizer, logger, this);
        this.synchronizer.setServerConnectionManager(serverConnectionManager);

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
        synchronizer.stop();
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
        //logger.logInfo("Server " + serverName +" (server index " + serverIndex + ") started on " + addresses.get(serverIndex).first() + ":" + addresses.get(serverIndex).second());
        logger.logInfo("Server '" + serverName +"' started on " + addresses.get(serverIndex).first() + ":" + addresses.get(serverIndex).second());
        logger.logInfo("Server '" + serverName +"' started on Thread " + Thread.currentThread().getName() );
        while(!stop){
        }
        stopThreads();
        //logger.logInfo("Server " + serverName +" (server index " + serverIndex + ") stopped");
        logger.logInfo("Server " + serverName +" stopped");
    }

    public void stopServer(){
        stop = true;
    }

    public String getServerName(){
        return serverName;
    }
}