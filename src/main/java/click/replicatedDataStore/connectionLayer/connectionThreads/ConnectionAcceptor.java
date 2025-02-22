package click.replicatedDataStore.connectionLayer.connectionThreads;

import click.replicatedDataStore.connectionLayer.connectionManagers.ConnectionManager;

import java.net.ServerSocket;

public class ConnectionAcceptor extends Thread{
    private final ServerSocket serverSocket;
    private final ConnectionManager connectionManager;

    ConnectionAcceptor (ConnectionManager connectionManager, ServerSocket serverSocket){
        this.connectionManager = connectionManager;
        this.serverSocket = serverSocket;
    }
    public void run(){}
}
