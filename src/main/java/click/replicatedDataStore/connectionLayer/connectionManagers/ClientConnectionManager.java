package click.replicatedDataStore.connectionLayer.connectionManagers;

import click.replicatedDataStore.applicationLayer.serverComponents.ClientServerPriorityQueue;
import click.replicatedDataStore.applicationLayer.Logger;
import click.replicatedDataStore.applicationLayer.serverComponents.dataManager.DataManagerReader;
import click.replicatedDataStore.connectionLayer.CommunicationMethods;
import click.replicatedDataStore.connectionLayer.connectionThreads.ClientsHandler;
import click.replicatedDataStore.connectionLayer.connectionThreads.ConnectionHandler;
import click.replicatedDataStore.connectionLayer.messages.*;
import click.replicatedDataStore.dataStructures.ClientWrite;

import java.io.IOException;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class ClientConnectionManager extends ConnectionManager {
    private final List<ClientsHandler> clientsHandlerList;
    private final ClientServerPriorityQueue que;
    private final DataManagerReader dataRead;

    /**
     * @param clientPort the port on which the server accepts connections from clients
     * @param serverQueue the server priority queue on which incoming data is sent
     * @param dataRead the data manager reader
     * @param logger the server logger
     */
    public ClientConnectionManager(int clientPort, ClientServerPriorityQueue serverQueue,
                            DataManagerReader dataRead, Logger logger) {
        super(clientPort, logger);
        this.clientsHandlerList = new LinkedList<>();
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
            ClientsHandler newClient = new ClientsHandler(newConnection, this);
            newClient.start();
            clientsHandlerList.add(newClient);
        } catch (IOException e){
            this.logger.logErr(this.getClass(), "error while creating a new connected client\n" + e.getMessage());
        }
    }

    @Override
    public void handleClosingConnection(ConnectionHandler handler) {
        boolean result = this.clientsHandlerList.remove(handler);
    }

    private Optional<AbstractMsg<?>> readData(AbstractMsg<?> msg){
        ClientReadMsg read = (ClientReadMsg) msg;
        return Optional.of(new ClientWriteMsg(new ClientWrite(read.getPayload(), dataRead.clientRead(read.getPayload()))));
    }

    private Optional<AbstractMsg<?>> writeData(AbstractMsg<?> msg){
        ClientWriteMsg write = (ClientWriteMsg) msg;
        return Optional.of(new StateAnswerMsg(que.addClientData(write.getPayload())? AnswerState.OK: AnswerState.FAIL));
    }

    public void stop(){
        super.stop();
        for(ClientsHandler client: clientsHandlerList){
            client.stopRunning();
        }
    }
}
