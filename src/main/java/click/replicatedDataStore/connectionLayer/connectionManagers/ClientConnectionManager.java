package click.replicatedDataStore.connectionLayer.connectionManagers;

import click.replicatedDataStore.applicationLayer.serverComponents.ClientServerPriorityQueue;
import click.replicatedDataStore.applicationLayer.serverComponents.Logger;
import click.replicatedDataStore.applicationLayer.serverComponents.dataManager.DataManagerReader;
import click.replicatedDataStore.connectionLayer.CommunicationMethods;
import click.replicatedDataStore.connectionLayer.connectionThreads.ConnectedClients;
import click.replicatedDataStore.connectionLayer.messages.*;

import java.io.IOException;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;

public class ClientConnectionManager extends ConnectionManager {
    private final List<ConnectedClients> connectedClientsList;
    private final ClientServerPriorityQueue que;
    private final DataManagerReader dataRead;

    /**
     * @param port the port on which the server accepts connections from clients
     * @param serverQueue the server priority queue on which incoming data is sent
     * @param dataRead the data manager reader
     * @param logger the server logger
     */
    ClientConnectionManager(int port, ClientServerPriorityQueue serverQueue,
                            DataManagerReader dataRead, Logger logger) {
        //TODO create connection acceptor thread
        super(port, logger);
        this.connectedClientsList = new LinkedList<>();
        this.que = serverQueue;
        this.dataRead = dataRead;
    }

    /**
     * used by the super class to set up the routing table
     */
    protected void setupRouting() {
        this.routingTable.put(CommunicationMethods.CLIENT_READ, this::readData);
        this.routingTable.put(CommunicationMethods.CLIENT_WRITE, this::writeData);
    }

    /**
     * method called by the ConnectionAcceptor after a new connection from a client
     * @param newConnection the new client connection
     */
    public synchronized void handleNewConnection(Socket newConnection){
        try {
            connectedClientsList.add(new ConnectedClients(newConnection, this));
        } catch (IOException e){
            this.logger.logErr(this.getClass(), "error while creating a new connected client\n" + e.getMessage());
        }
    }

    private AbstractMsg readData(AbstractMsg msg){
        ClientReadMsg read = (ClientReadMsg) msg;
        return new ClientWriteMsg(read.getPayload(), dataRead.read(read.getPayload()));
    }

    private AbstractMsg writeData(AbstractMsg msg){
        ClientWriteMsg write = (ClientWriteMsg) msg;
        return new StateAnswerMsg(que.addClientData(write.getPayload())? AnswerState.OK: AnswerState.FAIL);
    }


    public void stop(){
        super.stop();
        for(ConnectedClients client: connectedClientsList){
            client.stopRunning();
        }
    }
}
