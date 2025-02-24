package click.replicatedDataStore.applicationLayer;

import click.replicatedDataStore.applicationLayer.serverComponents.DataManager.DataManagerReader;
import click.replicatedDataStore.applicationLayer.serverComponents.DataManager.DataManagerWriter;
import click.replicatedDataStore.applicationLayer.serverComponents.Persist;
import click.replicatedDataStore.applicationLayer.serverComponents.TimeTravel;
import click.replicatedDataStore.dataStructures.VectorClock;
import click.replicatedDataStore.dataStructures.ClockedData;
import click.replicatedDataStore.utlis.Key;
import click.replicatedDataStore.dataStructures.Pair;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class Server {
    private final Map<Integer, Pair<Integer, Integer>> addresses;
    private final int ID;
    private final VectorClock vectorClock;
    private final LinkedHashMap<Key, Objects> primaryIndex;
    private final LinkedHashMap<VectorClock, Key> secondaryIndex;
    private final DataManagerReader dataManagerReader;
    private final DataManagerWriter dataManagerWriter;
    private final Persist persist;
    private final TimeTravel timeTravel;

    public Server(int ID, int serverNumber) {
        this.ID = ID;
        this.vectorClock = new VectorClock(serverNumber, ID);
        //todo
        this.addresses = null;
        this.primaryIndex = null;
        this.secondaryIndex = null;
        this.dataManagerReader = null;
        this.dataManagerWriter = null;
        this.persist = null;
        this.timeTravel = null;
    }

    public void updateAndPersist(ClockedData clockedData) {
        //todo
    }

    public synchronized VectorClock getVectorClock() {
        return vectorClock;
    }

    public synchronized VectorClock getOffsetVectorClock(int offset) {
        //todo
        return null;
    }

    public void stopThreads() {
        //todo
    }
}
