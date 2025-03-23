package click.replicatedDataStore.dataStructures.keyImplementations;

import click.replicatedDataStore.utils.Key;

public class StringKey implements Key {
    public final String key;

    public StringKey(String key) {
        this.key = key;
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof StringKey) {
            return key.equals(((StringKey) obj).key);
        }
        return false;
    }

    @Override
    public String toString() {
        return key;
    }
}
