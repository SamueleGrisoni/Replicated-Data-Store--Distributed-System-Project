package click.replicatedDataStore.dataStructures;

import click.replicatedDataStore.utlis.Key;

public record ClockedData(VectorClock vectorClock, Key key, Object value) implements Comparable<ClockedData> {

    @Override
    public int compareTo(ClockedData o) {
        return vectorClock.compareTo(o.vectorClock);
    }
}
