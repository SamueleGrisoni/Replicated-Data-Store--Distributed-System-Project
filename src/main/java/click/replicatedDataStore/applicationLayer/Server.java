package click.replicatedDataStore.applicationLayer;

import click.replicatedDataStore.applicationLayer.serverComponents.dataManager.DataManagerReader;
import click.replicatedDataStore.applicationLayer.serverComponents.dataManager.DataManagerWriter;
import click.replicatedDataStore.applicationLayer.serverComponents.Persist;
import click.replicatedDataStore.applicationLayer.serverComponents.TimeTravel;
import click.replicatedDataStore.connectionLayer.connectionManagers.ServerConnectionManager;
import click.replicatedDataStore.dataStructures.ClientWrite;
import click.replicatedDataStore.dataStructures.ClockedData;
import click.replicatedDataStore.dataStructures.VectorClock;
import click.replicatedDataStore.utlis.ServerConfig;
import click.replicatedDataStore.utlis.Key;
import click.replicatedDataStore.dataStructures.Pair;

import java.io.Serializable;
import java.util.*;

public class Server {
    private final Map<Integer, Pair<String, Integer>> addresses;
    private final int serverID;
    private final VectorClock vectorClock;
    private final LinkedHashMap<Key, Serializable> primaryIndex;
    private final TreeMap<VectorClock, Key> secondaryIndex;
    private final DataManagerReader dataManagerReader;
    private final DataManagerWriter dataManagerWriter;
    private final ServerConnectionManager serverConnectionManager;
    private final Logger logger;
    private final TimeTravel timeTravel;
    private final Persist persist;

    //todo make the serverNumber a config parameter. Vector clocks take it from config
    //todo move the clock in a separate class. server just a holder and initializer
    public Server(int serverID, Map<Integer, Pair<String, Integer>> addresses) {
        this.serverID = serverID;
        this.addresses = addresses;
        int serverNumber = addresses.size();
        this.vectorClock = new VectorClock(serverNumber, serverID);
        this.logger = new Logger();

        String dataFolderName = ServerConfig.DATA_FOLDER_NAME+serverID;
        String primaryIndexFileName = ServerConfig.PRIMARY_INDEX_FILE_NAME + serverID + ServerConfig.FILES_EXTENSION;
        String secondaryIndexFileName = ServerConfig.SECONDARY_INDEX_FILE_NAME + serverID + ServerConfig.FILES_EXTENSION;
        this.persist = new Persist(dataFolderName, primaryIndexFileName, secondaryIndexFileName);

        this.primaryIndex = persist.recoverPrimaryIndex();
        this.secondaryIndex = persist.recoverSecondaryIndex();

        //If secondary index is not empty, update the vector clock with the latest clock. Useful for recovery
        if(!secondaryIndex.isEmpty()){
            vectorClock.updateClock(secondaryIndex.lastKey());
        }

        this.dataManagerWriter = new DataManagerWriter(this);
        this.dataManagerWriter.start();

        this.dataManagerReader = new DataManagerReader(this);
        this.serverConnectionManager = new ServerConnectionManager()
        TimeTravel timeTravel = new TimeTravel(this, dataManagerReader, );
        System.out.println("Server " + serverID + " started on " + ServerConfig.getServerAddress(serverID).first() + ":" + ServerConfig.getServerAddress(serverID).second());
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

    public void stopThreads() {
        //todo
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

    public Pair<String, Integer> getMyAddressAndPortPair(){
        return addresses.get(serverID);
    }

    public Pair<String, Integer> getAddressAndPortPairOf(int serverID){
        return addresses.get(serverID);
    }

    //todo remove this and substitute the one made by @author Sam
    public List<Integer> getLowerServers(){
        return null;
    }

    public int getNumberOfServers(){
        return addresses.size();
    }

    public int getServerID(){
        return Integer.valueOf(this.serverID);
    }

    public Set<Integer> getOtherIndexes(){
        Set<Integer> list = addresses.keySet();
        list.remove(this.serverID);
        return list;
    }

    public void addClientData(ClientWrite clientWrite){
        dataManagerWriter.addClientData(clientWrite);
    }

    public void addServerData(List<ClockedData> serverData){
        dataManagerWriter.addServerData(serverData);
    }

    public ServerConnectionManager getServerConnectionManager() {
        return serverConnectionManager;
    }
}
