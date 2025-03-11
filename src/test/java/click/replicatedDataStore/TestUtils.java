package click.replicatedDataStore;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class TestUtils {

    static final int MIN_PORT = 49152;
    static final int MAX_PORT = 49200;
    static int serverPort;

    public static int getPort() {
        try (ServerSocket serverSocket = new ServerSocket(0)){
            serverPort = serverSocket.getLocalPort();
            serverSocket.close();
            Thread.sleep(500);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return serverPort;
    }
}
