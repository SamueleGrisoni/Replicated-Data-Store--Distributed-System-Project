package click.replicatedDataStore.connectionLayer.connectionThreads;

import click.replicatedDataStore.connectionLayer.connectionManagers.ConnectionManager;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public abstract class ConnectionHandler extends Thread{
    protected final ObjectOutputStream out;
    protected final ObjectInputStream in;
    protected final ConnectionManager manager;
    protected boolean running = true;

    protected ConnectionHandler(Socket socket, ConnectionManager manager) throws IOException{
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.in = new ObjectInputStream(socket.getInputStream());
        this.manager = manager;
    }

    public abstract void run();
    protected void stopRunning(){
        running = false;
    }
}
