package click.replicatedDataStore.applicationLayer.serverComponents;

public class VectorClock {
    private final int[] clock;

    public VectorClock(int serverNumber) {
        clock = new int[serverNumber];
    }
}
