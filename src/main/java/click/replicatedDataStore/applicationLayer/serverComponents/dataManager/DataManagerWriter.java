package click.replicatedDataStore.applicationLayer.serverComponents.dataManager;

import click.replicatedDataStore.applicationLayer.serverComponents.ClientServerPriorityQueue;
import click.replicatedDataStore.applicationLayer.serverComponents.ExternalConsistencySynchronizer;
import click.replicatedDataStore.applicationLayer.serverComponents.ServerDataSynchronizer;
import click.replicatedDataStore.dataStructures.ClientWrite;
import click.replicatedDataStore.dataStructures.ClockedData;
import click.replicatedDataStore.dataStructures.Pair;
import click.replicatedDataStore.dataStructures.VectorClock;
import click.replicatedDataStore.utils.ClockTooFarAhead;
import click.replicatedDataStore.utils.DataType;
import click.replicatedDataStore.utils.configs.ServerConfig;

import java.util.List;

public class DataManagerWriter extends Thread {
    private final ServerDataSynchronizer serverDataSynchronizer;
    private final ClientServerPriorityQueue queue;
    private int numberOfWrites = 0;
    private ExternalConsistencySynchronizer externalConsistencySynchronizer;
    private boolean stop = false;

    public DataManagerWriter(ServerDataSynchronizer serverDataSynchronizer) {
        this.serverDataSynchronizer = serverDataSynchronizer;
        this.queue = new ClientServerPriorityQueue(this.serverDataSynchronizer);
    }

    public void setTimeTravel(ExternalConsistencySynchronizer externalConsistencySynchronizer) {
        this.externalConsistencySynchronizer = externalConsistencySynchronizer;
    }

    @Override
    public void run() {
        //System.out.println("Writer thread of server " + serverDataSynchronizer.getServerIndex() + " started");
        while (!stop) {
            //Lock the queue so new data cannot be added while writing
            queue.lockQueue();
            try {
                Pair<DataType, List<ClockedData>> data = queue.pollData();
                processPopData(data);
            } finally {
                queue.unlockQueue();
            }
        }
    }

    private void processPopData(Pair<DataType, List<ClockedData>> data) {
        if(data.first() == DataType.CLIENT){
            System.out.println("Sending data to other servers" + data.second());
            externalConsistencySynchronizer.heavyPush(data.second());
            write(data.second());
        }else if(data.first() == DataType.SERVER){
            try {
                for (ClockedData clockedData : data.second()) {
                    VectorClock.checkIfUpdatable(serverDataSynchronizer.getServerIndex(), serverDataSynchronizer.getVectorClock(), clockedData.vectorClock());
                    write(List.of(clockedData));
                }
            }catch (ClockTooFarAhead e){
                System.out.println("Server"+serverDataSynchronizer.getServerIndex() + ": incoming data is too far ahead. Discarding Update");
            }
        }else{
            throw new IllegalArgumentException("Unknown data type: " + data.first());
        }
    }

    private void write(List<ClockedData> clockedDataList) {
        for (ClockedData clockedData : clockedDataList) {
            numberOfWrites++;
            if (numberOfWrites % ServerConfig.NUMBER_OF_WRITE_BETWEEN_SECONDARY_INDEX_UPDATE == 0) {
                serverDataSynchronizer.updateAndPersistSecondaryIndex(clockedData);
                numberOfWrites = 0;
            }
            serverDataSynchronizer.updateAndPersistPrimaryIndex(clockedData);
            serverDataSynchronizer.updateAndPersistVectorClock(clockedData.vectorClock());
            serverDataSynchronizer.addToBackupList(clockedData);
        }
    }

    public void stopThread() {
        stop = true;
        queue.lockQueue();
        try {
            // Unblock thread if it is waiting for data
            queue.getNotEmptyCondition().signalAll();
        } finally {
            queue.unlockQueue();
        }
    }

    public ClientServerPriorityQueue getQueue() {
        return queue;
    }

    //Methods use for tests
    public void addClientData(ClientWrite clientWrite) {
        queue.addClientData(clientWrite);
    }

    public void addServerData(List<ClockedData> serverData) {
        queue.addServerData(serverData);
    }
}
