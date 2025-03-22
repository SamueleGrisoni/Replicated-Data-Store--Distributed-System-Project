package click.replicatedDataStore.connectionLayer.connectionManagers;

import click.replicatedDataStore.applicationLayer.Server;
import click.replicatedDataStore.applicationLayer.serverComponents.Logger;
import click.replicatedDataStore.applicationLayer.serverComponents.TimeTravel;
import click.replicatedDataStore.connectionLayer.CommunicationMethods;
import click.replicatedDataStore.connectionLayer.connectionThreads.*;
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
        super(server.getMyAddressAndPorts().second().serverPort(), logger);
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
        IntStream otherServerIndexes = IntStream.range(0, server.getNumberOfServers()).filter(i -> i!=server.getServerIndex());
        otherServerIndexes.forEach(index -> {
            this.serverHandlersMap.put(index, Optional.empty());
            this.handlerLocksMap.put(index, new Object());
        });
    }

    @Override
    public void handleNewConnection(Socket newConnection) {
        Thread passiveHandlerCreator = new Thread(()-> {
            try {
                //the Server handler constructor will put itself in the map
                ServerHandler newServer = new PassiveServerHandler(newConnection, this);
                newServer.start();
            }catch(ConnectionCreateTimeOutException timeOut){
                this.logger.logErr(this.getClass(), timeOut.getMessage());
            }catch (IOException e) {
                this.logger.logErr(this.getClass(), "error while creating a new connected client\n" + e.getMessage());
            }
        });
        passiveHandlerCreator.start();
    }

    @Override
    public void handleClosingConnection(ConnectionHandler handler) {
        for(Map.Entry<Integer, Optional<ServerHandler>> entry : serverHandlersMap.entrySet()){
            if(entry.getValue().isPresent() && entry.getValue().get().equals(handler)){
                serverHandlersMap.put(entry.getKey(), Optional.empty());
                break;
            }
        }
    }

    private void createConnections(){
        IntStream serverIndexes = IntStream.range(0, server.getNumberOfServers())
                .filter(index -> index != server.getServerIndex());
        serverIndexes.forEach(index -> {
            Thread activeHandlerCreator = new Thread(() -> {
                Pair<String, ServerPorts> ipPort = server.getAddressAndPortsPairOf(index);
                try {
                    Socket socket = new Socket(ipPort.first(), ipPort.second().serverPort());
                    ActiveServerHandler serverHandler;
                    synchronized (handlerLocksMap.get(index)) {
                        serverHandler = new ActiveServerHandler(socket, this, this.server.getServerIndex(), index);
                    }
                    serverHandler.start();
                }catch(ConnectionCreateTimeOutException timeOut){
                    this.logger.logErr(this.getClass(), timeOut.getMessage());
                }catch (IOException e){
                    this.logger.logErr(this.getClass(),
                            "error cant open socket on" + ipPort.first() + ":" + ipPort.second() + "\n" +
                                    e.getMessage());
                    }
            });
            activeHandlerCreator.start();
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

    public void stop(){
        super.stop();
        serverHandlersMap.values().forEach(opt -> opt.ifPresent(ServerHandler::stopRunning));
    }

    public void addIndexed(Integer index, ServerHandler serverHandler){
        synchronized (handlerLocksMap.get(index)) {
            serverHandlersMap.get(index).ifPresent(ServerHandler::stopRunning);
            this.serverHandlersMap.put(index, Optional.of(serverHandler));
            logger.logInfo("new connection with server " + index);
        }
    }
}
