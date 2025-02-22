package click.replicatedDataStore.dataStructures;

import click.replicatedDataStore.applicationLayer.serverComponents.VectorClock;
import click.replicatedDataStore.utlis.Key;

public class ClockedData {
    final VectorClock vectorClock;
    final Key key;
    final Object value;

    public ClockedData(VectorClock vectorClock, Key key, Object value) {
        this.vectorClock = vectorClock;
        this.key = key;
        this.value = value;
    }

    public int Comparator(ClockedData other) {
        //todo
        return 0;
    }
}
