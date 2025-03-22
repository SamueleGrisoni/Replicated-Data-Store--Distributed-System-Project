package click.replicatedDataStore.connectionLayer.connectionThreads;

public class ConnectionCreateTimeOutException extends RuntimeException {
    public ConnectionCreateTimeOutException(String message) {
        super("Time out over creating a new connection\n" + message);
    }
}
