package click.replicatedDataStore.applicationLayer.serverComponents;

import click.replicatedDataStore.dataStructures.ClockedData;
import click.replicatedDataStore.dataStructures.VectorClock;
import click.replicatedDataStore.utlis.Key;
import click.replicatedDataStore.utlis.ServerConfig;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TreeMap;

public class ServerDataSynchronizer {
    private final int serverNumber;
    private final int serverID;

    private final VectorClock vectorClock;
    private final LinkedHashMap<Key, Serializable> primaryIndex;
    private final TreeMap<VectorClock, Key> secondaryIndex;
    private final Persist persist;

    public ServerDataSynchronizer(int serverNumber, int serverID){
        this.serverNumber = serverNumber;
        this.serverID = serverID;
        this.persist= persistInitializer();
        this.primaryIndex = persist.recoverPrimaryIndex();
        this.secondaryIndex = persist.recoverSecondaryIndex();
        this.vectorClock = vectorClockInitializer();
    }

    private Persist persistInitializer(){
        String dataFolderName = ServerConfig.DATA_FOLDER_NAME+serverID;
        String primaryIndexFileName = ServerConfig.PRIMARY_INDEX_FILE_NAME + serverID + ServerConfig.FILES_EXTENSION;
        String secondaryIndexFileName = ServerConfig.SECONDARY_INDEX_FILE_NAME + serverID + ServerConfig.FILES_EXTENSION;
        return new Persist(dataFolderName, primaryIndexFileName, secondaryIndexFileName);
    }

    //If secondaryIndex is not empty, update the vector clock with the latest clock. Useful for recovery
    private VectorClock vectorClockInitializer(){
        VectorClock vectorClock = new VectorClock(serverNumber, serverID);
        if(!secondaryIndex.isEmpty()){
            vectorClock.updateClock(secondaryIndex.lastKey());
        }
        return vectorClock;
    }

    public void updateAndPersist(List<ClockedData> clockedDataList) {
        synchronized (primaryIndex) {
            for(ClockedData clockedData : clockedDataList){
                updatePersistPrimaryIndex(clockedData);
            }
            vectorClock.updateClock(clockedDataList.get(clockedDataList.size()-1).vectorClock());
        }
    }

    public void updateAndPersist(List<ClockedData> clockedDataList, TreeMap<VectorClock, Key> secondaryIndexUpdated) {
        synchronized (primaryIndex) {
            for(int i = 0; i<clockedDataList.size()-1; i++){
                updatePersistPrimaryIndex(clockedDataList.get(i));
            }
            ClockedData lastClockedData = clockedDataList.get(clockedDataList.size()-1);
            synchronized (secondaryIndex) {
                persist.persist(lastClockedData, secondaryIndexUpdated);
                primaryIndex.remove(lastClockedData.key());
                primaryIndex.put(lastClockedData.key(), lastClockedData.value());
                secondaryIndex.clear();
                secondaryIndex.putAll(secondaryIndexUpdated);
            }
            vectorClock.updateClock(lastClockedData.vectorClock());
        }
    }

    private void updatePersistPrimaryIndex(ClockedData clockedData){
        synchronized (primaryIndex) {
            persist.persist(clockedData);
            primaryIndex.remove(clockedData.key());
            primaryIndex.put(clockedData.key(), clockedData.value());
        }
    }

    //As discussed during design, the synchronization is probably not needed here
    //(Writer and Queue are already synchronized). It is kept just in case
    public synchronized VectorClock getVectorClock() {
        return vectorClock;
    }

    public synchronized VectorClock getOffsetVectorClock(int offset) {
        return new VectorClock(vectorClock, offset);
    }

    //return a COPY of the primary index
    public LinkedHashMap<Key, Serializable> getPrimaryIndex() {
        synchronized (primaryIndex){
            return new LinkedHashMap<>(primaryIndex);
        }
    }

    //return a COPY of the secondary index
    public TreeMap<VectorClock, Key> getSecondaryIndex() {
        synchronized (secondaryIndex){
            return new TreeMap<>(secondaryIndex);
        }
    }

    public int getServerID() {
        return serverID;
    }
}
