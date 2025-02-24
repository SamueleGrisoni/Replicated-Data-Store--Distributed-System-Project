package click.replicatedDataStore.dataStructures;

import click.replicatedDataStore.utlis.Key;

public record ClockedData(VectorClock vectorClock, Key key, Object value) {
}
