package click.replicatedDataStore.utlis;

public interface Key {
    int hashCode();
    boolean equals(Object obj);
    String toString();
}
