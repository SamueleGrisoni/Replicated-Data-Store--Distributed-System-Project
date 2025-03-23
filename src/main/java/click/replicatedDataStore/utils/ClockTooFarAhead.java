package click.replicatedDataStore.utils;

public class ClockTooFarAhead extends RuntimeException {
    public ClockTooFarAhead(String message) {
        super(message);
    }
}
