package click.replicatedDataStore.utlis;

public class ClockTooFarAhead extends RuntimeException {
    public ClockTooFarAhead(String message) {
        super(message);
    }
}
