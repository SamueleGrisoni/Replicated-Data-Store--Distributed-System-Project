package click.replicatedDataStore.connectionLayer.connectionManagers;

import click.replicatedDataStore.applicationLayer.Logger;
import click.replicatedDataStore.connectionLayer.CommunicationMethods;
import click.replicatedDataStore.connectionLayer.connectionThreads.ConnectionAcceptor;
import click.replicatedDataStore.connectionLayer.messages.AbstractMsg;

import java.net.Socket;
import java.util.HashMap;
import java.util.function.Function;

public abstract class ConnectionManager {
    private final ConnectionAcceptor connectionAcceptor;
    protected final HashMap<CommunicationMethods, Function<AbstractMsg, AbstractMsg>> routingTable = new HashMap<>();
    public final Logger logger;

    /**
     * @param port the port on which the server accepts connections
     * @param logger the logger for the server
     */
    protected ConnectionManager(int port, Logger logger){
        this.logger = logger;
        connectionAcceptor = new ConnectionAcceptor(port, this);
        setupRouting();

        connectionAcceptor.start();
    }

    /**
     * Used to set up the routing table by the class constructor
     */
    protected abstract void setupRouting();

    /**
     * Routes the message to the correct method and returns the response
     * @param msg the message to be routed
     * @return the response to the message
     */
    public AbstractMsg resolveRequest(AbstractMsg msg){
        return routingTable.get(msg.method).apply(msg);
    }

    public abstract void handleNewConnection(Socket newConnection);

    protected void stop(){
        connectionAcceptor.stopRunning();
    }
}
