package click.replicatedDataStore.applicationLayer.serverComponents.dataManager;

import click.replicatedDataStore.applicationLayer.serverComponents.ServerDataSynchronizer;
import click.replicatedDataStore.dataStructures.ClockedData;
import click.replicatedDataStore.dataStructures.VectorClock;
import click.replicatedDataStore.utils.Key;

import java.io.Serializable;
import java.util.*;

public class DataManagerReader{
    private final ServerDataSynchronizer serverDataSynchronizer;

    //Se si vuole un DataManagerReader per client, ha senso avere un DataManagerReaderClient per client e un DataManagerReaderServer, entrabe con una queue di richieste.
    //DataManagerReaderClient ha una queue di Key, DataManagerReaderServer ha una queue di ClockedData (o ServerWrite)
    public DataManagerReader(ServerDataSynchronizer serverDataSynchronizer) {
        this.serverDataSynchronizer = serverDataSynchronizer;
    }

    //A client request to read a key
    public Serializable clientRead(Key key){
        return serverDataSynchronizer.getPrimaryIndex().get(key);
    }

    // Recover Data return a List of ClockedData.
    // If I have the right VectorClock use that, else, use max vectorClock in secondaryIndex older than the update
    public List<ClockedData> recoverData(VectorClock otherVectorClock){
        System.out.println("Recovering data for VectorClock: " + otherVectorClock);
        LinkedHashMap<Key, Serializable> primaryIndex = serverDataSynchronizer.getPrimaryIndex();
        TreeMap<VectorClock, Key> secondaryIndex = serverDataSynchronizer.getSecondaryIndex();
        Key startKey = secondaryIndex.get(otherVectorClock);
        Iterator<Map.Entry<Key, Serializable>> primaryIndexIterator = primaryIndex.entrySet().iterator();

        while(primaryIndexIterator.hasNext()){
            Map.Entry<Key, Serializable> startingEntry = primaryIndexIterator.next();
            //Iterate until the startKey is found, after that add every entry to the dataToRecover list
            if(startingEntry.getKey().equals(startKey)){
                return retrieveDataFromPrimaryIndex(primaryIndexIterator, startKey, secondaryIndex, startingEntry);
            }
        }
        //If the startKey is not found, return an empty list
        return new ArrayList<>();
    }

    private List<ClockedData> retrieveDataFromPrimaryIndex(Iterator<Map.Entry<Key, Serializable>> primaryIndexIterator, Key startKey, TreeMap<VectorClock, Key> secondaryIndex, Map.Entry<Key, Serializable> startingEntry) {
        List<ClockedData> dataToRecover = new ArrayList<>();
        VectorClock maxSecondaryInClock = computeSecondaryIndexClock(secondaryIndex, startKey);
        dataToRecover.add(new ClockedData(maxSecondaryInClock, startingEntry.getKey(), startingEntry.getValue()));
        while(primaryIndexIterator.hasNext()){
            Map.Entry<Key, Serializable> entry = primaryIndexIterator.next();
            VectorClock vc = computeSecondaryIndexClock(secondaryIndex, entry.getKey());
            if(vc == null) {
                dataToRecover.add(new ClockedData(maxSecondaryInClock, entry.getKey(), entry.getValue()));
            }else{
                dataToRecover.add(new ClockedData(vc, entry.getKey(), entry.getValue()));
                maxSecondaryInClock = vc;
            }
        }
        return dataToRecover;
    }

    private VectorClock computeSecondaryIndexClock(TreeMap<VectorClock, Key> secondaryIndex, Key key) {
        return secondaryIndex.entrySet().stream().filter(entry -> entry.getValue().equals(key)).findFirst().get().getKey();
    }
}
