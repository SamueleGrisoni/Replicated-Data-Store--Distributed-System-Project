package click.replicatedDataStore.connectionLayer.connectionManagers;

import click.replicatedDataStore.applicationLayer.Server;
import click.replicatedDataStore.applicationLayer.serverComponents.ClientServerPriorityQueue;
import click.replicatedDataStore.applicationLayer.serverComponents.Logger;
import click.replicatedDataStore.applicationLayer.serverComponents.TimeTravel;
import click.replicatedDataStore.connectionLayer.CommunicationMethods;
import click.replicatedDataStore.connectionLayer.messages.AbstractMsg;
import click.replicatedDataStore.connectionLayer.messages.ServerFetchMsg;
import click.replicatedDataStore.connectionLayer.messages.ServerHeavyPushMsg;
import click.replicatedDataStore.dataStructures.ClockedData;
import click.replicatedDataStore.dataStructures.VectorClock;

import java.net.Socket;
import java.util.List;

public class ServerConnectionManager extends ConnectionManager{
    private final ClientServerPriorityQueue que;
    private final TimeTravel sync;
    private final Server server;

    public ServerConnectionManager(Integer port, ClientServerPriorityQueue que, TimeTravel sync,
                                   Logger logger, Server server) {
        super(port, logger);

        this.que = que;
        this.sync = sync;
        this.server = server;
    }

    @Override
    public void setupRouting() {

    }

    @Override
    public void handleNewConnection(Socket newConnection) {
        this.routingTable.put(CommunicationMethods.SERVER_H_PUSH, this::heavyPush);
        this.routingTable.put(CommunicationMethods.SERVER_L_PUSH, this::lightPush);
        this.routingTable.put(CommunicationMethods.SERVER_FETCH, this::fetch);
    }

    public AbstractMsg heavyPush(AbstractMsg msg){
        return null;
    }
    public void heavyPush(List<ClockedData> heavier){

    }

    public AbstractMsg lightPush(AbstractMsg msg){
        return null;
    }
    public void lightPush(VectorClock light){

    }

    public AbstractMsg fetch(AbstractMsg msg){
        ServerFetchMsg fetch = (ServerFetchMsg) msg;
        List<ClockedData> list = sync.computeFetch(fetch.getPayload().first());
        return new ServerHeavyPushMsg(list);
    }
    public void fetch(VectorClock current){

    }
}
