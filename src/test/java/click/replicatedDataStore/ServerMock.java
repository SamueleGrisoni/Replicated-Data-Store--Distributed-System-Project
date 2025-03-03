package click.replicatedDataStore;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.BiFunction;
import java.util.function.Function;

public class ServerMock {
    private final Thread serverThread;
    private ServerSocket socket;

    private final BiFunction<ObjectInputStream, ObjectOutputStream, Void> callbackFunction;

    /**
     * @param serverPort the port to listen on
     * @param callback the function to call when a connection is made
     */
    public ServerMock(int serverPort, BiFunction callback) {
        this.callbackFunction = callback;
        serverThread = new Thread(() -> {
            ServerSocket serverSocket = null;
            Socket socket = null;
            ObjectOutputStream out = null;
            ObjectInputStream in = null;

            try {
                serverSocket = new ServerSocket(serverPort);

                socket = serverSocket.accept();

                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());

            } catch (Exception e) {
                e.printStackTrace();
            }

            this.socket = serverSocket;
            callbackFunction.apply(in, out);

            try {
                socket.close();
            } catch (IOException e) {}
        });
    }

    public void start() {
        serverThread.start();
    }
    public void stop() {
        serverThread.interrupt();
        try {
            if(socket!=null)
                this.socket.close();
        } catch (IOException e) {
        }
    }
}
