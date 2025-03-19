package click.replicatedDataStore.connectionLayer.connectionManagers;

import click.replicatedDataStore.applicationLayer.Server;
import click.replicatedDataStore.applicationLayer.Logger;
import click.replicatedDataStore.applicationLayer.serverComponents.TimeTravel;
import click.replicatedDataStore.connectionLayer.CommunicationMethods;
import click.replicatedDataStore.connectionLayer.connectionThreads.ActiveServerHandler;
import click.replicatedDataStore.connectionLayer.connectionThreads.PassiveServerHandler;
import click.replicatedDataStore.connectionLayer.connectionThreads.ServerHandler;
import click.replicatedDataStore.connectionLayer.messages.*;
import click.replicatedDataStore.dataStructures.Pair;
import click.replicatedDataStore.dataStructures.ServerPorts;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

public class ServerConnectionManager extends ConnectionManager{
    private final TimeTravel sync;
    private final Server server;
    private final Map<Integer, Optional<ServerHandler>> serverHandlersMap = new HashMap<>();
    private final Map<Integer, Object> handlerLocksMap = new HashMap<>();

    public ServerConnectionManager(TimeTravel sync,
                                   Logger logger, Server server) {
        super(server.getMyAddressAndPorts().second().incomingPort(),
                server.getMyAddressAndPorts().second().outgoingPort(),
                logger);

        this.sync = sync;
        this.server = server;
        initializeServerHandlerAndLocksMap();
        this.createConnections();
    }

    @Override
    public void setupRouting() {
        this.routingTable.put(CommunicationMethods.SERVER_H_PUSH, this::handleHeavyPush);
        this.routingTable.put(CommunicationMethods.SERVER_L_PUSH, this::handleLightPush);
        this.routingTable.put(CommunicationMethods.SERVER_FETCH, this::handleFetch);
    }

    private void initializeServerHandlerAndLocksMap(){
        IntStream otherServerIndexes = IntStream.range(0, server.getNumberOfServers()).filter(i -> i!=server.getServerID());
        otherServerIndexes.forEach(index -> {
            this.serverHandlersMap.put(index, Optional.empty());
            this.handlerLocksMap.put(index, new Object());
        });
    }

    @Override
    public void handleNewConnection(Socket newConnection) {
        try {
            //the Server handler constructor will put itself in the map
            ServerHandler newServer = new PassiveServerHandler(newConnection, this);
            newServer.start();
        } catch (IOException e){
            this.logger.logErr(this.getClass(), "error while creating a new connected client\n" + e.getMessage());
        }
    }


    private void createConnections(){
        IntStream serverIndexes = IntStream.range(0, server.getNumberOfServers())
                .filter(index -> index != server.getServerID());
        serverIndexes.forEach(index -> {
            Thread t = new Thread(() -> {
                //todo fix this with the new 2 ports system
                Pair<String, ServerPorts> ipPort = server.getAddressAndPortsPairOf(index);
                try {
                    Socket socket = new Socket(ipPort.first(), ipPort.second().incomingPort());
                    ActiveServerHandler serverHandler;
                    synchronized (handlerLocksMap.get(index)) {
                        serverHandler = new ActiveServerHandler(socket, this, this.server.getServerID(), index);
                    }
                    serverHandler.start();
                } catch (IOException e){
                    this.logger.logErr(this.getClass(),
                                  "error cant open socket on" + ipPort.first() + ":" + ipPort.second() + "\n" +
                                       e.getMessage());
                }
            });
            t.start();
        });
    }

    public void sendMessage(AbstractMsg<?> msg, int recipientIndex){
        serverHandlersMap.get(recipientIndex).ifPresent(serverHandler -> serverHandler.sendMessage(msg));
    }

    public void broadcastMessage(AbstractMsg<?> msg){
        serverHandlersMap.values()
                .forEach(opt-> opt.ifPresent(handler -> handler.sendMessage(msg)));
    }

    public Optional<AbstractMsg<?>> handleHeavyPush(AbstractMsg<?> msg){
        ServerHeavyPushMsg hPush = (ServerHeavyPushMsg) msg;
        return sync.handleHeavyPush(hPush);
    }

    public Optional<AbstractMsg<?>> handleLightPush(AbstractMsg<?> msg){
        ServerLightPushMsg lPush = (ServerLightPushMsg) msg;
        return sync.handleLightPush(lPush);
    }

    public Optional<AbstractMsg<?>> handleFetch(AbstractMsg<?> msg){
        ServerFetchMsg fetch = (ServerFetchMsg) msg;
        return sync.handleFetch(fetch);
    }

    //TODO call this in server
    public void stop(){
        super.stop();
        serverHandlersMap.values().forEach(opt -> opt.ifPresent(ServerHandler::stopRunning));
    }

    public void addIndexed(Integer index, ServerHandler serverHandler){
        synchronized (handlerLocksMap.get(index)) {
            serverHandlersMap.get(index).ifPresent(ServerHandler::stopRunning);
            this.serverHandlersMap.put(index, Optional.of(serverHandler));
        }
    }
}
