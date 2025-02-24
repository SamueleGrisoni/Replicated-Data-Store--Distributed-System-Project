package click.replicatedDataStore.applicationLayer;

import click.replicatedDataStore.applicationLayer.serverComponents.DataManager.DataManagerReader;
import click.replicatedDataStore.applicationLayer.serverComponents.DataManager.DataManagerWriter;
import click.replicatedDataStore.applicationLayer.serverComponents.Persist;
import click.replicatedDataStore.applicationLayer.serverComponents.TimeTravel;
import click.replicatedDataStore.dataStructures.VectorClock;
import click.replicatedDataStore.dataStructures.ClockedData;
import click.replicatedDataStore.utlis.Config;
import click.replicatedDataStore.utlis.Key;
import click.replicatedDataStore.dataStructures.Pair;

import java.util.*;

public class Server {
    private final Map<Integer, Pair<Integer, Integer>> addresses;
    private final int serverID;
    private final VectorClock vectorClock;
    private final LinkedHashMap<Key, Object> primaryIndex;
    private final LinkedHashMap<VectorClock, Key> secondaryIndex;
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
        this.addresses = null;
        this.dataManagerReader = null;
        this.dataManagerWriter = null;
        this.timeTravel = null;

        String dataFolderName = Config.DATA_FOLDER_NAME;
        String primaryIndexFileName = Config.PRIMARY_INDEX_FILE_NAME + serverID + Config.FILES_EXTENSION;
        String secondaryIndexFileName = Config.SECONDARY_INDEX_FILE_NAME + serverID + Config.FILES_EXTENSION;
        this.persist = new Persist(dataFolderName, primaryIndexFileName, secondaryIndexFileName);

        this.primaryIndex = persist.recoverPrimaryIndex();
        this.secondaryIndex = persist.recoverSecondaryIndex();



        System.out.println("Server " + serverID + " started on " + Config.getServerAddress(serverID).first() + ":" + Config.getServerAddress(serverID).second());
    }

    public void updateAndPersist(ClockedData clockedData) {
        //todo
        writeLock.notify();
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
}
