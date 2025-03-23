package click.replicatedDataStore.utils;

import java.io.Serializable;

public interface Key extends Serializable {
    int hashCode();
    boolean equals(Object obj);
    String toString();
}
