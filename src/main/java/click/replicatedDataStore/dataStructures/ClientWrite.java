package click.replicatedDataStore.dataStructures;

import click.replicatedDataStore.utlis.Key;

public record ClientWrite(Key key, Object value) {
}
