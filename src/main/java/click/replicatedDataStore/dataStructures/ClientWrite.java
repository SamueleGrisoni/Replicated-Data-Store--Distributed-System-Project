package click.replicatedDataStore.dataStructures;

import click.replicatedDataStore.utlis.Key;

import java.io.Serializable;

public record ClientWrite(Key key, Serializable value) implements Serializable {
}
