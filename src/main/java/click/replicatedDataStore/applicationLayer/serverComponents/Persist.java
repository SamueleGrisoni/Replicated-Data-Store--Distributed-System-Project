package click.replicatedDataStore.applicationLayer.serverComponents;

import click.replicatedDataStore.dataStructures.ClockedData;
import click.replicatedDataStore.utlis.Key;

import java.util.LinkedHashMap;
import java.util.Objects;

public class Persist {
    private final String path;

    public Persist(String path) {
        this.path = path;
    }

    public void persist(ClockedData clockedData) {
        //todo
    }

    public LinkedHashMap<Key, Objects> recoverPrimaryIndex() {
        //todo
        return null;
    }

    public LinkedHashMap<VectorClock, Key> recoverSecondaryIndex() {
        //todo
        return null;
    }
}
