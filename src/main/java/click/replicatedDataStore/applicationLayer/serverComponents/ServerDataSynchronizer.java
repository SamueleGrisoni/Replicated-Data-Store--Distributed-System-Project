package click.replicatedDataStore.applicationLayer.serverComponents;

import click.replicatedDataStore.dataStructures.ClockedData;
import click.replicatedDataStore.dataStructures.VectorClock;
import click.replicatedDataStore.utils.Key;
import click.replicatedDataStore.utils.configs.ServerConfig;

import java.io.Serializable;
import java.util.*;

public class ServerDataSynchronizer {
    private final int serverNumber;
    private final int serverIndex;
    private final String serverName;

    private final VectorClock vectorClock;
    private final LinkedHashMap<Key, Serializable> primaryIndex;
    private final TreeMap<VectorClock, Integer> secondaryIndex;
    private final BackupList backupList;
    private Persist persist;
    private final Boolean isPersistent;
    public ServerDataSynchronizer(String serverName, int serverNumber, int serverIndex, Boolean isPersistent) {
        this.serverName = serverName;
        this.serverNumber = serverNumber;
        this.serverIndex = serverIndex;
        this.isPersistent = isPersistent;
        this.persist = persistInitializer();
        this.primaryIndex = recoverPrimaryIndex();
        this.secondaryIndex = recoverSecondaryIndex();
        this.backupList = recoverBackupList();
        this.vectorClock = recoverVectorClock(serverName, serverNumber, serverIndex);
    }

    public void updateAndPersistPrimaryIndex(ClockedData clockedData) {
        synchronized (primaryIndex) {
            primaryIndex.remove(clockedData.key());
            primaryIndex.put(clockedData.key(), clockedData.value());
            if (isPersistent) {
                persist.persist(clockedData);
            }
        }
    }

    public void updateAndPersistVectorClock(VectorClock incomingClock) {
        synchronized (vectorClock) {
            vectorClock.updateClock(incomingClock);
            if (isPersistent){
                persist.persist(vectorClock);
                //System.out.println("Persisted clock: " + vectorClock);
            }
        }
    }

    public void updateAndPersistSecondaryIndex(ClockedData clockedData) {
        synchronized (secondaryIndex) {
            secondaryIndex.put(clockedData.vectorClock(), backupList.size());
            if (isPersistent) {
                persist.persist(secondaryIndex);
            }
        }
    }

    public void addToBackupList(ClockedData clockedData) {
        synchronized (backupList) {
            backupList.add(clockedData);
        }
        if (isPersistent) {
            persist.persist(backupList);
        }
    }

    private Persist persistInitializer(){
        if(!isPersistent) {
            return null;
        }
        String dataFolderName = ServerConfig.SERVER_DATA_FOLDER_NAME + serverIndex;
        String primaryIndexFileName = ServerConfig.PRIMARY_INDEX_FILE_NAME + serverIndex + ServerConfig.FILES_EXTENSION;
        String secondaryIndexFileName = ServerConfig.SECONDARY_INDEX_FILE_NAME + serverIndex + ServerConfig.FILES_EXTENSION;
        String vectorClockFileName = ServerConfig.VECTOR_CLOCK_FILE_NAME + serverIndex + ServerConfig.FILES_EXTENSION;
        String backupListFileName = ServerConfig.BACKUP_LIST_FILE_NAME + serverIndex + ServerConfig.FILES_EXTENSION;
        return new Persist(dataFolderName, primaryIndexFileName, secondaryIndexFileName, vectorClockFileName, backupListFileName);
    }

    private LinkedHashMap<Key, Serializable> recoverPrimaryIndex(){
        if (isPersistent){
            return persist.recoverPrimaryIndex();
        }else{
            return new LinkedHashMap<>();
        }
    }

    private BackupList recoverBackupList(){
        if (isPersistent){
            return persist.recoverBackupList();
        }else{
            return new BackupList();
        }
    }

    private TreeMap<VectorClock, Integer> recoverSecondaryIndex(){
        if (isPersistent){
            return persist.recoverSecondaryIndex();
        }else{
            return new TreeMap<>();
        }
    }

    private VectorClock recoverVectorClock(String serverName, int serverNumber, int serverIndex){
        if (isPersistent) {
            return persist.recoverVectorClock(serverName, serverNumber, serverIndex);
        } else {
            return new VectorClock(serverName, serverNumber, serverIndex);
        }
    }


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
    public TreeMap<VectorClock, Integer> getSecondaryIndex() {
        synchronized (secondaryIndex){
            return new TreeMap<>(secondaryIndex);
        }
    }

    //return a COPY of the backup list
    public BackupList getBackupList() {
        synchronized (backupList) {
            return new BackupList(backupList);
        }
    }

    public int getServerIndex() {
        return serverIndex;
    }

    public int getServerNumber() {
        return serverNumber;
    }

    public String getServerName() {
        return serverName;
    }
}
