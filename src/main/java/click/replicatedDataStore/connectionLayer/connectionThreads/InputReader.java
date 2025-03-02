package click.replicatedDataStore.connectionLayer.connectionThreads;

import click.replicatedDataStore.connectionLayer.connectionManagers.ConnectionManager;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class InputReader extends Thread {
    private final ObjectInputStream in;
    private final ConnectionManager manager;
    public boolean running = true;
    private final Queue<Object> messageQueue = new ConcurrentLinkedQueue<>();

    public InputReader(Socket socket, ConnectionManager manager) {
        this.manager = manager;
        ObjectInputStream tmpIn = null;

        try {
            tmpIn = new ObjectInputStream(socket.getInputStream());
        }catch (IOException e) {
            e.printStackTrace();
        } finally {
            in = tmpIn;
        }
    }

    public Object readMessage() {
        return messageQueue.poll();
    }

    public void run() {
        while(running) {
            Object nextMessage = messageQueue.poll();
            if(nextMessage != null) {

            }
        }
        tearDown();
    }

    private void tearDown() {

    }
}
