package click.replicatedDataStore.connectionLayer.connectionThreads;

import click.replicatedDataStore.ServerGlobalParameters;
import click.replicatedDataStore.connectionLayer.connectionManagers.ConnectionManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ConnectionAcceptor extends Thread{
    private final ServerSocket serverSocket;
    private final ConnectionManager connectionManager;
    public boolean running = true;

    /**
     * @param port the port on which the server accepts connections from clients
     * @param connectionManager the connection manager
     */
    public ConnectionAcceptor (int port, ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
        ServerSocket serverSocket = null;
        do {
            try {
                serverSocket = new ServerSocket(port);
            } catch (IOException e) {
                connectionManager.logger.logErr(this.getClass(), "error while opening server socket on port " + port + "\n" + e.getMessage());

                try {
                    sleep(ServerGlobalParameters.retryToOpenServerSocketMilliseconds);
                } catch (InterruptedException ex) {
                    throw new RuntimeException("error while sleeping\n" + ex.getMessage());
                }
            }
        } while (serverSocket == null && running);

        this.serverSocket = serverSocket;
    }

    public void run() {
        while(running) {
            try {
                Socket newClientConnection = serverSocket.accept(); //todo: if i get two connection at the same time?
                connectionManager.handleNewConnection(newClientConnection);
            } catch (IOException e) {
                connectionManager.logger.logErr(this.getClass(), "error while accepting client connection\n" + e.getMessage());
            }
        }
    }

    public void stopRunning() {
        this.running = false;
        try {
            serverSocket.close();
        } catch (IOException e) {
            connectionManager.logger.logErr(this.getClass(), "error while closing server socket\n" + e.getMessage());
        }
    }
}
