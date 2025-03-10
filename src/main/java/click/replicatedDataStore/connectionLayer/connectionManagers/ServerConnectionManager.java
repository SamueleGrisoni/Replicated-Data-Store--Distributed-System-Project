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

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

public class ServerConnectionManager extends ConnectionManager{
    private final TimeTravel sync;
    private final Server server;
    private final Map<Integer, ServerHandler> serverHandlersMap = new HashMap<>();

    public ServerConnectionManager(Integer port, TimeTravel sync,
                                   Logger logger, Server server) {
        super(port, logger);

        this.sync = sync;
        this.server = server;
        this.createConnections();
    }

    @Override
    public void setupRouting() {
        this.routingTable.put(CommunicationMethods.SERVER_H_PUSH, sync::handleHeavyPush);
        this.routingTable.put(CommunicationMethods.SERVER_L_PUSH, sync::handleLightPush);
        this.routingTable.put(CommunicationMethods.SERVER_FETCH, sync::handleFetch);
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
        IntStream serverIndexes = IntStream.range(0, server.getNumberOfServers());
        int myId = server.getServerID();
        serverIndexes.filter(index -> index != myId).forEach(index -> {
            Thread t = new Thread(() -> {
                Pair<String, Integer> ipPort = server.getAddressAndPortPairOf(index);
                try{
                    Socket socket = new Socket(ipPort.first(), ipPort.second());
                    //the Server handler constructor will put itself in the map
                    ActiveServerHandler serverHandler = new ActiveServerHandler(socket, this, index);
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
        serverHandlersMap.get(recipientIndex).sendMessage(msg);
    }

    public void broadcastMessage(AbstractMsg<?> msg){
        for (Integer i : server.getOtherIndexes()){
            serverHandlersMap.get(i).sendMessage(msg);
        }
    }

    public void stop(){
        super.stop();
        for(ServerHandler serverHandler: serverHandlersMap.values()){
            serverHandler.stopRunning();
        }
    }

    public void addIndexed(Integer index, ServerHandler serverHandler){
        if(this.serverHandlersMap.containsKey(index)){
            ServerHandler oldHandler = serverHandlersMap.get(index);
            oldHandler.stopRunning();
        }
        this.serverHandlersMap.put(index, serverHandler);
    }
}
