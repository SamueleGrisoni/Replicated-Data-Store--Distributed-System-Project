package click.replicatedDataStore.applicationLayer.serverComponents.dataManager;

import click.replicatedDataStore.applicationLayer.Server;
import click.replicatedDataStore.dataStructures.ClockedData;
import click.replicatedDataStore.dataStructures.Pair;
import click.replicatedDataStore.dataStructures.VectorClock;
import click.replicatedDataStore.utlis.Key;

import java.util.*;

public class DataManagerReader{
    private final Server server;

    //Se si vuole un DataManagerReader per client, ha senso avere un DataManagerReaderClient per client e un DataManagerReaderServer, entrabe con una queue di richieste.
    //DataManagerReaderClient ha una queue di Key, DataManagerReaderServer ha una queue di ClockedData (o ServerWrite)
    public DataManagerReader(Server server) {
        this.server = server;
    }

    //A client request to read a key
    public Object clientRead(Key key){
        return server.getPrimaryIndex().get(key);
    }

    // Recover Data return a List of ClockedData.
    // If I have the right VectorClock use that, else, use max vectorClock in secondaryIndex older than the update
    public List<ClockedData> recoverData(VectorClock otherVectorClock){
        LinkedHashMap<Key, Object> primaryIndex = server.getPrimaryIndex();
        TreeMap<VectorClock, Key> secondaryIndex = server.getSecondaryIndex();
        Key startKey = secondaryIndex.get(otherVectorClock);
        List<ClockedData> dataToRecover = new ArrayList<>();
        Iterator<Map.Entry<Key, Object>> primaryIndexIterator = primaryIndex.entrySet().iterator();
        while(primaryIndexIterator.hasNext()){
            Map.Entry<Key, Object> entry = primaryIndexIterator.next();
            //Iterate until the startKey is found, after that add every entry to the dataToRecover list
            if(entry.getKey().equals(startKey)){
                VectorClock maxSecondaryInClock = computeSecondaryIndexClock(secondaryIndex, startKey);
                dataToRecover.add(new ClockedData(maxSecondaryInClock, entry.getKey(), entry.getValue()));
                while(primaryIndexIterator.hasNext()){
                    VectorClock vc = computeSecondaryIndexClock(secondaryIndex, entry.getKey());
                    if(vc == null) {
                        dataToRecover.add(new ClockedData(maxSecondaryInClock, entry.getKey(), entry.getValue()));
                    }else{
                        dataToRecover.add(new ClockedData(vc, entry.getKey(), entry.getValue()));
                        maxSecondaryInClock = vc;
                    }
                }
                break;
            }
        }
        return dataToRecover;
    }

    private VectorClock computeSecondaryIndexClock(TreeMap<VectorClock, Key> secondaryIndex, Key key) {
        return secondaryIndex.entrySet().stream().filter(entry -> entry.getValue().equals(key)).findFirst().get().getKey();
    }
}
