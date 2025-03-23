package click.replicatedDataStore.dataStructures;

import click.replicatedDataStore.utils.Key;

import java.io.Serializable;

public record ClientWrite(Key key, Serializable value) implements Serializable {
}
