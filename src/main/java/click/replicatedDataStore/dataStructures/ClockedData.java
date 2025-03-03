package click.replicatedDataStore.dataStructures;

import click.replicatedDataStore.utlis.Key;

import java.io.Serializable;

public record ClockedData(VectorClock vectorClock, Key key, Serializable value) implements Comparable<ClockedData> {

    @Override
    public int compareTo(ClockedData o) {
        return vectorClock.compareTo(o.vectorClock);
    }
}
