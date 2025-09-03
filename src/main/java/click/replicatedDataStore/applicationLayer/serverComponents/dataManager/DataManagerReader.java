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
    public DataManagerReader(ServerDataSynchronizer serverDataSynchronizer) {
        this.serverDataSynchronizer = serverDataSynchronizer;
    }

    //A client request to read a key
    public Serializable clientRead(Key key) {
        return serverDataSynchronizer.getPrimaryIndex().get(key);
    }

    //Recover all data past a given VectorClock
    public List<ClockedData> recoverData(VectorClock otherVectorClock) {
        System.out.println("Recovering data for VectorClock: " + otherVectorClock);
        List<ClockedData> clockedDataList = new ArrayList<>();
        BackupList backupList = serverDataSynchronizer.getBackupList();
        TreeMap<VectorClock, Integer> secondaryIndex = serverDataSynchronizer.getSecondaryIndex();

        if (secondaryIndex.isEmpty()) {
            clockedDataList.addAll(backupList.getAllData());
        } else {
            int startIndex = computeStartIndex(otherVectorClock, secondaryIndex);
            List<ClockedData> dataSinceIndex = backupList.getClockedDataSinceIndex(startIndex);

            for (ClockedData clockedData : dataSinceIndex) {
                if (isDataNewer(clockedData, otherVectorClock)) {
                    clockedDataList.add(clockedData);
                }
            }
        }

        System.out.println("Recovered data: " + BackupList.printClockDataList(clockedDataList));
        return clockedDataList;
    }

    private boolean isDataNewer(ClockedData clockedData, VectorClock otherVectorClock) {
        int compareRes = clockedData.compareTo(otherVectorClock);
        return compareRes != VectorClockComparation.LESS_THAN.getCompareResult() && compareRes != VectorClockComparation.EQUAL.getCompareResult();
    }

    private int computeStartIndex(VectorClock otherVectorClock, TreeMap<VectorClock, Integer> secondaryIndex) {
        Map.Entry<VectorClock, Integer> entry = secondaryIndex.lowerEntry(otherVectorClock);
        return (entry != null) ? entry.getValue() : 0;
    }
}
