package click.replicatedDataStore.connectionLayer.connectionThreads;

import click.replicatedDataStore.connectionLayer.connectionManagers.ClientConnectionManager;

import java.io.IOException;
import java.net.Socket;

public class ClientsHandler extends ConnectionHandler{

    public ClientsHandler(Socket clientSocket, ClientConnectionManager manager) throws IOException{
        super(clientSocket, manager);
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
