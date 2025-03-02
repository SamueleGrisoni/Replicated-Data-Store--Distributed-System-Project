package click.replicatedDataStore.connectionLayer.connectionThreads;

import click.replicatedDataStore.connectionLayer.connectionManagers.ClientConnectionManager;
import click.replicatedDataStore.connectionLayer.messages.AbstractMsg;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ConnectedClients extends Thread{
    private final ObjectOutputStream out;
    private final ObjectInputStream in;
    private final ClientConnectionManager manager;
    private boolean running = true;

    public ConnectedClients(Socket clientSocket, ClientConnectionManager manager) throws IOException{
        out = new ObjectOutputStream(clientSocket.getOutputStream());
        in = new ObjectInputStream(clientSocket.getInputStream());
        this.manager = manager;
    }

    public void run(){
        while(running){
            try {
                AbstractMsg request = (AbstractMsg) in.readObject();
                out.writeObject(manager.resolveRequest(request));
            }catch (IOException e){
                manager.logger.logErr(this.getClass(), "error while processing a request\n" + e.getMessage());
            }catch (ClassNotFoundException e){
                manager.logger.logErr(this.getClass(), "error the input from the socket isn't an AbstractMsg\n" + e.getMessage());
            }
        }
    }

    public void stopRunning(){
        running = false;
        try {
            out.close();
            in.close();
        } catch (IOException e){
            manager.logger.logErr(this.getClass(), "error while closing client connection\n" + e.getMessage());
        }
    }
}
