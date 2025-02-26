package click.replicatedDataStore.applicationLayer;

import click.replicatedDataStore.applicationLayer.serverComponents.dataManager.DataManagerReader;
import click.replicatedDataStore.applicationLayer.serverComponents.dataManager.DataManagerWriter;
import click.replicatedDataStore.applicationLayer.serverComponents.Persist;
import click.replicatedDataStore.applicationLayer.serverComponents.TimeTravel;
import click.replicatedDataStore.dataStructures.ClockedData;
import click.replicatedDataStore.dataStructures.VectorClock;
import click.replicatedDataStore.utlis.ClockTooFarAhead;
import click.replicatedDataStore.utlis.Config;
import click.replicatedDataStore.utlis.Key;
import click.replicatedDataStore.dataStructures.Pair;

import java.util.*;

public class Server {
    private final Map<Integer, Pair<String, Integer>> addresses;
    private final int serverID;
    private final VectorClock vectorClock;
    private final LinkedHashMap<Key, Object> primaryIndex;
    private final TreeMap<VectorClock, Key> secondaryIndex;
    private final DataManagerReader dataManagerReader;
    private final DataManagerWriter dataManagerWriter;

    private final TimeTravel timeTravel;
    private final Object writeLock = new Object();
    private final List<Object> readLocks = new ArrayList<>();
    private final Persist persist;

    public Server(int serverID, int serverNumber) {
        this.serverID = serverID;
        this.vectorClock = new VectorClock(serverNumber, serverID);
        //todo
        this.addresses = Config.addresses;
        this.dataManagerReader = null;
        this.dataManagerWriter = null;
        this.timeTravel = null;

        String dataFolderName = Config.DATA_FOLDER_NAME;
        String primaryIndexFileName = Config.PRIMARY_INDEX_FILE_NAME + serverID + Config.FILES_EXTENSION;
        String secondaryIndexFileName = Config.SECONDARY_INDEX_FILE_NAME + serverID + Config.FILES_EXTENSION;
        this.persist = new Persist(dataFolderName, primaryIndexFileName, secondaryIndexFileName);

        this.primaryIndex = persist.recoverPrimaryIndex();
        this.secondaryIndex = persist.recoverSecondaryIndex();
        if(!secondaryIndex.isEmpty()){
            vectorClock.updateClock(secondaryIndex.lastKey());
        }

        System.out.println("Server " + serverID + " started on " + Config.getServerAddress(serverID).first() + ":" + Config.getServerAddress(serverID).second());
    }

    //Locking on the function parameters is not a good practice.
    //The server will be responsible for updating and locking its own maps and vectorClock
    public void updateAndPersist(ClockedData clockedData, Map<Key, Object> primaryIndexUpdate) {
        synchronized (primaryIndex){
            primaryIndex.clear();
            primaryIndex.putAll(primaryIndexUpdate);
            persist.persist(primaryIndex);
            try{
                vectorClock.updateClock(clockedData.vectorClock());
            }catch (ClockTooFarAhead e){
                //this should never happen, because clockedData was already applied to the primaryIndexUpdate
                e.printStackTrace();
            }
        }
        primaryIndex.notifyAll();
    }

    public void updateAndPersist(ClockedData clockedData, Map<Key, Object> primaryIndexUpdate, Map<VectorClock, Key> secondaryIndexUpdate) {
        synchronized (primaryIndex){
            synchronized (secondaryIndex){
                primaryIndex.clear();
                primaryIndex.putAll(primaryIndexUpdate);
                secondaryIndex.clear();
                secondaryIndex.putAll(secondaryIndexUpdate);
                persist.persist(primaryIndex, secondaryIndex);
                try{
                    vectorClock.updateClock(clockedData.vectorClock());
                }catch (ClockTooFarAhead e){
                    //this should never happen, because clockedData was already applied to the primaryIndexUpdate
                    e.printStackTrace();
                }
            }
        }
        primaryIndex.notifyAll();
    }

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
    public LinkedHashMap<Key, Object> getPrimaryIndex() {
        synchronized (primaryIndex){
            return new LinkedHashMap<>(primaryIndex);
        }
    }

    //return a COPY of the secondary index
    public LinkedHashMap<VectorClock, Key> getSecondaryIndex() {
        synchronized (secondaryIndex){
            return new LinkedHashMap<>(secondaryIndex);
        }
    }

    public Pair<String, Integer> getMyAddressAndPortPair(){
        return addresses.get(serverID);
    }

    public Pair<String, Integer> getAddressAndPortPairOf(int serverID){
        return addresses.get(serverID);
    }
}
