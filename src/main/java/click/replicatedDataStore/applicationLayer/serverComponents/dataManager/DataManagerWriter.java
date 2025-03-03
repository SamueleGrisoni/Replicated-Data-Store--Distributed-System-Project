package click.replicatedDataStore.applicationLayer.serverComponents.dataManager;

import click.replicatedDataStore.applicationLayer.Server;
import click.replicatedDataStore.applicationLayer.serverComponents.ClientServerPriorityQueue;
import click.replicatedDataStore.dataStructures.ClientWrite;
import click.replicatedDataStore.dataStructures.ClockedData;
import click.replicatedDataStore.dataStructures.VectorClock;
import click.replicatedDataStore.utlis.Config;
import click.replicatedDataStore.utlis.Key;

import java.util.*;

public class DataManagerWriter extends Thread{
    private final Server server;
    private final ClientServerPriorityQueue queue;
    private int numberOfWrites = 0;

    public DataManagerWriter(Server server) {
        this.server = server;
        this.queue = new ClientServerPriorityQueue(server);
    }

    @Override
    public void run() {
        System.out.println("Writer thread started");
        while(true){
            write(queue.peekData());
            //Pop the data after it has been written
            queue.popData();
        }
    }

    private void write(ClockedData clockedData){
        numberOfWrites++;
        if (numberOfWrites % Config.NUMBER_OF_WRITE_BETWEEN_SECONDARY_INDEX_UPDATE == 0) {
            writeSecondaryIndex(clockedData);
            numberOfWrites = 0;
        }else{
            server.updateAndPersist(clockedData);
        }
    }

    private void writeSecondaryIndex(ClockedData clockedData){
        Key updatingKey = clockedData.key();
        TreeMap<VectorClock, Key> secondaryIndex = server.getSecondaryIndex();
        //Iterate through the secondary index to find the key that is being updated.
        //Try to update that secondary index entry with the next entry in the primary index
        //If the next entry is already in the secondary index, remove the current secondary index entry
        if (secondaryIndex.containsValue(updatingKey)) {
            for(Map.Entry<VectorClock, Key> currSecondaryEntry : secondaryIndex.entrySet()){
                if(currSecondaryEntry.getValue().equals(updatingKey)){
                    Key newPossibleKey = computeNewPossibleKey(updatingKey);
                    //The newPossibleKey is null iff is the last entry in the primary index.
                    if(newPossibleKey == null || secondaryIndex.containsValue(newPossibleKey)) {
                        secondaryIndex.remove(currSecondaryEntry.getKey());
                    }else {
                        secondaryIndex.put(currSecondaryEntry.getKey(), newPossibleKey);
                    }
                    break;
                }
            }
        }
        secondaryIndex.put(clockedData.vectorClock(), updatingKey);
        server.updateAndPersist(clockedData, secondaryIndex);
    }

    //Return the entry after the updatingKey in the primary index
    private Key computeNewPossibleKey(Key updatingKey){
        //Remember: when initialized the iterator point to a position before the first element
        Iterator<Key> primaryIndexIterator = server.getPrimaryIndex().keySet().iterator();
        while(primaryIndexIterator.hasNext()){
            Key currKey = primaryIndexIterator.next();
            if(currKey.equals(updatingKey) && primaryIndexIterator.hasNext()){
                return primaryIndexIterator.next();
            }
        }
        return null;
    }

    public void addClientData(ClientWrite clientWrite){
        queue.addClientData(clientWrite);
    }

    public void addServerData(ClockedData serverData){
        queue.addServerData(serverData);
    }
}
