package click.replicatedDataStore.connectionLayer.connectionManagers;

import click.replicatedDataStore.applicationLayer.Server;
import click.replicatedDataStore.applicationLayer.serverComponents.ClientServerPriorityQueue;
import click.replicatedDataStore.applicationLayer.serverComponents.Logger;
import click.replicatedDataStore.applicationLayer.serverComponents.TimeTravel;
import click.replicatedDataStore.connectionLayer.CommunicationMethods;
import click.replicatedDataStore.connectionLayer.connectionThreads.ActiveServerHandler;
import click.replicatedDataStore.connectionLayer.connectionThreads.PassiveServerHandler;
import click.replicatedDataStore.connectionLayer.connectionThreads.ServerHandler;
import click.replicatedDataStore.connectionLayer.messages.*;
import click.replicatedDataStore.dataStructures.ClockedData;
import click.replicatedDataStore.dataStructures.Pair;
import click.replicatedDataStore.dataStructures.VectorClock;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ServerConnectionManager extends ConnectionManager{
    private final ClientServerPriorityQueue que;
    private final TimeTravel sync;
    private final Server server;
    private final Map<Integer, ServerHandler> serverHandlersMap = new HashMap<>();

    //todo setup recovery mechanisms
    public ServerConnectionManager(Integer port, ClientServerPriorityQueue que, TimeTravel sync,
                                   Logger logger, Server server) {
        super(port, logger);

        this.que = que;
        this.sync = sync;
        this.server = server;
        this.createConnections();
    }

    @Override
    public void setupRouting() {
        this.routingTable.put(CommunicationMethods.SERVER_H_PUSH, this::handleHeavyPush);
        this.routingTable.put(CommunicationMethods.SERVER_L_PUSH, this::handleLightPush);
        this.routingTable.put(CommunicationMethods.SERVER_FETCH, this::fetch);
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
        List<Integer> serverIndexes = server.getLowerServers();
        serverIndexes.forEach(index -> {
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

    public Optional<AbstractMsg> handleHeavyPush(AbstractMsg msg){
        ServerHeavyPushMsg hPush = (ServerHeavyPushMsg) msg;
        return Optional.empty();
    }

    public Optional<AbstractMsg> handleLightPush(AbstractMsg msg){
        ServerLightPushMsg lPush = (ServerLightPushMsg) msg;
        return sync.checkOutOfDate(lPush.getPayload())?
                Optional.of(new ServerFetchMsg(this.server.getVectorClock())) :
                Optional.empty();
    }

    public Optional<AbstractMsg> fetch(AbstractMsg msg){
        ServerFetchMsg fetch = (ServerFetchMsg) msg;
        List<ClockedData> list = sync.computeFetch(fetch.getPayload());
        if(!list.isEmpty())
            return Optional.of(new ServerHeavyPushMsg(list));
        else
            return Optional.empty();
    }

    public void heavyPush(List<ClockedData> heavy){
        for (Integer i : server.getOtherIndexes()){
            serverHandlersMap.get(i).sendMessage(new ServerHeavyPushMsg(heavy));
        }
    }
    public void lightPush(VectorClock light){
        for (Integer i : server.getOtherIndexes()){
            serverHandlersMap.get(i).sendMessage(new ServerLightPushMsg(light));
        }
    }

    public void stop(){
        super.stop();
        for(ServerHandler server: serverHandlersMap.values()){
            server.stopRunning();
        }
    }

    public void addIndexed(Integer index, ServerHandler serverHandler){
        this.serverHandlersMap.put(index, serverHandler);
    }
}
