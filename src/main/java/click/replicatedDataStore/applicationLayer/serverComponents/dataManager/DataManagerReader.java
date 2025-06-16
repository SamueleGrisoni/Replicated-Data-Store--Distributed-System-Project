package click.replicatedDataStore.applicationLayer.serverComponents.dataManager;

import click.replicatedDataStore.applicationLayer.serverComponents.ServerDataSynchronizer;
import click.replicatedDataStore.dataStructures.ClockedData;
import click.replicatedDataStore.dataStructures.Pair;
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
        LinkedHashMap<Key, Serializable> primaryIndex = serverDataSynchronizer.getPrimaryIndex();
        TreeMap<VectorClock, Key> secondaryIndex = serverDataSynchronizer.getSecondaryIndex();

        Key startKey = computeStartKey(otherVectorClock, secondaryIndex);
        if (startKey == null) {
            //System.out.println("Secondary index is empty, returning entire primary index.");
            List<ClockedData> dataToRecover = new ArrayList<>();
            for (Map.Entry<Key, Serializable> entry : primaryIndex.entrySet()) {
                dataToRecover.add(new ClockedData(new VectorClock(serverDataSynchronizer.getServerName(), serverDataSynchronizer.getServerNumber(), serverDataSynchronizer.getServerIndex()), entry.getKey(), entry.getValue()));
            }
            return dataToRecover;
        } else {
            //System.out.println("Secondary index is not empty, starting from key: " + startKey);
            return retrieveDataFromPrimaryIndex(primaryIndex, secondaryIndex, startKey);
        }
    }

    // Compute the startKey based on the otherVectorClock and the secondaryIndex. If the otherVectorClock is not found in the secondaryIndex,
    // return the greatest key in the secondaryIndex that is less than the otherVectorClock.
    // If no such key exists, return null.
    private Key computeStartKey(VectorClock otherVectorClock, TreeMap<VectorClock, Key> secondaryIndex) {
        if (secondaryIndex.isEmpty()) {
            return null;
        }
        Key startKey = secondaryIndex.get(otherVectorClock);
        if (startKey != null) { //otherVectorClock is in the secondaryIndex
            return startKey;
        } else {                  // Find the greatest key in the secondaryIndex that is less than the otherVectorClock
            Map.Entry<VectorClock, Key> entry = secondaryIndex.lowerEntry(otherVectorClock);
            if (entry != null) {
                return entry.getValue();
            } else {
                throw new IllegalStateException("No lower entry found for VectorClock: " + otherVectorClock + " in secondaryIndex but secondaryIndex is not empty " + secondaryIndex);
            }
        }
    }

    private List<ClockedData> retrieveDataFromPrimaryIndex(Map<Key, Serializable> primaryIndex, TreeMap<VectorClock, Key> secondaryIndex, Key startKey) {
        Pair<Map.Entry<Key, Serializable>, Iterator<Map.Entry<Key, Serializable>>> startingPoint = computePrimaryIndexStartingPoint(primaryIndex, startKey);
        Iterator<Map.Entry<Key, Serializable>> primaryIndexIterator = startingPoint.second();
        VectorClock maxSecondaryInClock = computeSecondaryIndexClock(secondaryIndex, startKey);
        //Add the first entry to the dataToRecover list
        List<ClockedData> dataToRecover = new ArrayList<>();
        dataToRecover.add(new ClockedData(maxSecondaryInClock, startingPoint.first().getKey(), startingPoint.first().getValue()));
        //Iterate over the primary index starting from the startKey and add every entry to the dataToRecover list
        while (primaryIndexIterator.hasNext()) {
            Map.Entry<Key, Serializable> entry = primaryIndexIterator.next();
            dataToRecover.add(new ClockedData(maxSecondaryInClock, entry.getKey(), entry.getValue()));
        }
        return dataToRecover;
    }

    // Return the vectorClock associated with the startKey.
    // If the startKey is null (so no vectorClock lower than the otherVectorClock exists in the secondaryIndex), returns a [0, 0, ..., 0] vectorClock.
    private VectorClock computeSecondaryIndexClock(TreeMap<VectorClock, Key> secondaryIndex, Key key) {
        Optional<Map.Entry<VectorClock, Key>> vectorClockKeyEntry = secondaryIndex.entrySet().stream().filter(entry -> entry.getValue().equals(key)).findFirst();
        if (vectorClockKeyEntry.isPresent()) {
            return vectorClockKeyEntry.get().getKey();
        } else {
            return new VectorClock(serverDataSynchronizer.getServerName(), serverDataSynchronizer.getServerNumber(), serverDataSynchronizer.getServerIndex());
        }
    }

    private Pair<Map.Entry<Key, Serializable>, Iterator<Map.Entry<Key, Serializable>>> computePrimaryIndexStartingPoint(Map<Key, Serializable> primaryIndex, Key startKey) {
        Iterator<Map.Entry<Key, Serializable>> primaryIndexIterator = primaryIndex.entrySet().iterator();
        while (primaryIndexIterator.hasNext()) {
            Map.Entry<Key, Serializable> entry = primaryIndexIterator.next();
            if (entry.getKey().equals(startKey)) {
                return new Pair<>(entry, primaryIndexIterator);
            }
        }
        throw new IllegalStateException("startKey " + startKey + " not found in primaryIndex " + primaryIndex);
    }
}
