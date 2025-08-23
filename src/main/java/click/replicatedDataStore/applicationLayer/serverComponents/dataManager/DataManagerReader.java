package click.replicatedDataStore.applicationLayer.serverComponents.dataManager;

import click.replicatedDataStore.applicationLayer.serverComponents.BackupList;
import click.replicatedDataStore.applicationLayer.serverComponents.ServerDataSynchronizer;
import click.replicatedDataStore.dataStructures.ClockedData;
import click.replicatedDataStore.dataStructures.VectorClock;
import click.replicatedDataStore.utils.Key;

import java.io.Serializable;
import java.util.*;

public class DataManagerReader {
    private final ServerDataSynchronizer serverDataSynchronizer;

    //Se si vuole un DataManagerReader per client, ha senso avere un DataManagerReaderClient per client e un DataManagerReaderServer, entrabe con una queue di richieste.
    //DataManagerReaderClient ha una queue di Key, DataManagerReaderServer ha una queue di ClockedData (o ServerWrite)
    public DataManagerReader(ServerDataSynchronizer serverDataSynchronizer) {
        this.serverDataSynchronizer = serverDataSynchronizer;
    }

    //A client request to read a key
    public Serializable clientRead(Key key) {
        return serverDataSynchronizer.getPrimaryIndex().get(key);
    }

    // Recover Data return a List of ClockedData starting from a startKey computed from the otherVectorClock and the secondaryIndex.
    // If the secondary index is empty, it returns the entire primary index.
    public List<ClockedData> recoverData(VectorClock otherVectorClock) {
        System.out.println("Recovering data for VectorClock: " + otherVectorClock);
        List<ClockedData> clockedDataList = new ArrayList<>();
        BackupList backupList = serverDataSynchronizer.getBackupList();

        for (ClockedData clockedData : backupList.getClockedDataSinceIndex(0)) {
            int compareRes = clockedData.compareTo(otherVectorClock);
            if (compareRes == VectorClockComparation.GREATER_THAN.getCompareResult() ||
                compareRes == VectorClockComparation.CONCURRENT.getCompareResult()) {
                clockedDataList.add(clockedData);
            }
        }
        return clockedDataList;
    }
}
