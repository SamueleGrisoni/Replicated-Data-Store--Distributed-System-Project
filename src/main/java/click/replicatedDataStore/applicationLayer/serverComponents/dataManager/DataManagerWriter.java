package click.replicatedDataStore.applicationLayer.serverComponents.dataManager;

import click.replicatedDataStore.applicationLayer.serverComponents.ClientServerPriorityQueue;
import click.replicatedDataStore.applicationLayer.serverComponents.ServerDataSynchronizer;
import click.replicatedDataStore.applicationLayer.serverComponents.TimeTravel;
import click.replicatedDataStore.dataStructures.ClientWrite;
import click.replicatedDataStore.dataStructures.ClockedData;
import click.replicatedDataStore.dataStructures.Pair;
import click.replicatedDataStore.dataStructures.VectorClock;
import click.replicatedDataStore.utlis.ClockTooFarAhead;
import click.replicatedDataStore.utlis.DataType;
import click.replicatedDataStore.utlis.Key;
import click.replicatedDataStore.utlis.ServerConfig;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class DataManagerWriter extends Thread {
    private final ServerDataSynchronizer serverDataSynchronizer;
    private final ClientServerPriorityQueue queue;
    private int numberOfWrites = 0;
    private TimeTravel timeTravel;
    private boolean stop = false;

    public DataManagerWriter(ServerDataSynchronizer serverDataSynchronizer) {
        this.serverDataSynchronizer = serverDataSynchronizer;
        this.queue = new ClientServerPriorityQueue(this.serverDataSynchronizer);
    }

    public void setTimeTravel(TimeTravel timeTravel) {
        this.timeTravel = timeTravel;
    }

    @Override
    public void run() {
        System.out.println("Writer thread started");
        while (true) {
            if(stop) break;
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
            timeTravel.heavyPush(data.second());
            write(data.second());
        }else if(data.first() == DataType.SERVER){
            try {
                VectorClock.checkIfUpdatable(serverDataSynchronizer.getServerID(), serverDataSynchronizer.getVectorClock(), data.second().get(0).vectorClock());
                write(data.second());
            }catch (ClockTooFarAhead e){
                System.out.println("Server"+serverDataSynchronizer.getServerID() + ": incoming data is too far ahead. Discarding Update");
            }
        }
    }

    private void write(List<ClockedData> clockedDataList) {
        numberOfWrites++;
        if (numberOfWrites % ServerConfig.NUMBER_OF_WRITE_BETWEEN_SECONDARY_INDEX_UPDATE == 0) {
            writeSecondaryIndex(clockedDataList);
            numberOfWrites = 0;
        } else {
            serverDataSynchronizer.updateAndPersist(clockedDataList);
        }
    }

    private void writeSecondaryIndex(List<ClockedData> clockedDataList) {
        ClockedData lastClockedData = clockedDataList.get(clockedDataList.size() - 1);
        Key updatingKey = lastClockedData.key();
        TreeMap<VectorClock, Key> secondaryIndex = serverDataSynchronizer.getSecondaryIndex();
        //Iterate through the secondary index to find the key that is being updated.
        //Try to update that secondary index entry with the next entry in the primary index
        //If the next entry is already in the secondary index, remove the current secondary index entry
        if (secondaryIndex.containsValue(updatingKey)) {
            for (Map.Entry<VectorClock, Key> currSecondaryEntry : secondaryIndex.entrySet()) {
                if (currSecondaryEntry.getValue().equals(updatingKey)) {
                    Key newPossibleKey = computeNewPossibleKey(updatingKey);
                    //The newPossibleKey is null iff is the last entry in the primary index.
                    if (newPossibleKey == null || secondaryIndex.containsValue(newPossibleKey)) {
                        secondaryIndex.remove(currSecondaryEntry.getKey());
                    } else {
                        secondaryIndex.put(currSecondaryEntry.getKey(), newPossibleKey);
                    }
                    break;
                }
            }
        }
        secondaryIndex.put(lastClockedData.vectorClock(), updatingKey);
        serverDataSynchronizer.updateAndPersist(clockedDataList, secondaryIndex);
    }

    //Return the entry after the updatingKey in the primary index
    private Key computeNewPossibleKey(Key updatingKey) {
        //Remember: when initialized the iterator point to a position before the first element
        Iterator<Key> primaryIndexIterator = serverDataSynchronizer.getPrimaryIndex().keySet().iterator();
        while (primaryIndexIterator.hasNext()) {
            Key currKey = primaryIndexIterator.next();
            if (currKey.equals(updatingKey) && primaryIndexIterator.hasNext()) {
                return primaryIndexIterator.next();
            }
        }
        return null;
    }

    public void addClientData(ClientWrite clientWrite) {
        queue.addClientData(clientWrite);
    }

    public void addServerData(List<ClockedData> serverData) {
        queue.addServerData(serverData);
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
}
