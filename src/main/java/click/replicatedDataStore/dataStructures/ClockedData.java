package click.replicatedDataStore.dataStructures;

import click.replicatedDataStore.utils.Key;

import java.io.Serializable;

public record ClockedData(VectorClock vectorClock, Key key, Serializable value) implements Comparable<ClockedData>, Serializable {

    @Override
    public int compareTo(ClockedData o) {
        return vectorClock.compareTo(o.vectorClock);
    }

    public int compareTo(VectorClock o) {
        return vectorClock.compareTo(o);
    }
}
