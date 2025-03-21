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
import click.replicatedDataStore.utlis.ServerInitializerUtils;

import java.util.*;

public class Server extends Thread{
    private final Map<Integer, Pair<String, ServerPorts>> addresses;
    private final int serverIndex;
    private final int serverNumber;

    private final ServerDataSynchronizer serverDataSynchronizer;
    private final DataManagerReader dataManagerReader;
    private final DataManagerWriter dataManagerWriter;
    private final ServerConnectionManager serverConnectionManager;
    private final ClientConnectionManager clientConnectionManager;
    private final TimeTravel timeTravel;
    private boolean stop = false;
    private final Logger logger = new Logger();

    public Server(int serverIndex, Map<Integer, Pair<String, ServerPorts>> addresses) {
        this.serverIndex = serverIndex;
        this.addresses = addresses;
        this.serverNumber = addresses.size();

        this.serverDataSynchronizer = new ServerDataSynchronizer(serverNumber, serverIndex);
        this.dataManagerWriter = new DataManagerWriter(serverDataSynchronizer);
        this.dataManagerReader = new DataManagerReader(serverDataSynchronizer);

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

    public Set<Integer> getOtherIndexes(){
        Set<Integer> list = addresses.keySet();
        list.remove(this.serverIndex);
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
        logger.logInfo("Server " + ServerInitializerUtils.getServerIdFromIndex(serverIndex) + " started on " + addresses.get(serverIndex).first() + ":" + addresses.get(serverIndex).second());
        while(!stop){
        }
        stopThreads();
    }

    public void stopServer(){
        stop = true;
    }
}