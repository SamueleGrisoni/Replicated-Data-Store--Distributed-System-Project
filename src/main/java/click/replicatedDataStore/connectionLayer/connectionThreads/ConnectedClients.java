package click.replicatedDataStore.connectionLayer.connectionThreads;

import click.replicatedDataStore.connectionLayer.connectionManagers.ClientConnectionManager;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ConnectedClients extends Thread{
    ObjectOutputStream out;
    ObjectInputStream in;

    ConnectedClients(Socket clientSocket, ClientConnectionManager manager){
        //TODO unpack socket
    }

    public void run(){
        //TODO implement run
    }
}
