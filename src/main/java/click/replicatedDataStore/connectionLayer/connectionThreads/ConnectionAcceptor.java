package click.replicatedDataStore.connectionLayer.connectionThreads;

import click.replicatedDataStore.connectionLayer.connectionManagers.ConnectionManager;
import click.replicatedDataStore.utlis.ServerConfig;

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
                    sleep(ServerConfig.retryToOpenServerSocketMilliseconds);
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
                Socket newClientConnection = serverSocket.accept();
                connectionManager.handleNewConnection(newClientConnection);
            } catch (IOException e) {
                if(running)
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
