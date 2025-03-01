package click.replicatedDataStore.connectionLayer.connectionManagers;

import click.replicatedDataStore.applicationLayer.serverComponents.ClientServerPriorityQueue;
import click.replicatedDataStore.applicationLayer.serverComponents.DataManager.DataManagerReader;
import click.replicatedDataStore.connectionLayer.connectionThreads.ConnectedClients;

import java.util.HashMap;
import java.util.List;

public class ClientConnectionManager extends ConnectionManager {
    private List<ConnectedClients> connectedClientsList;
    private final ClientServerPriorityQueue que;
    private final DataManagerReader dataRead;

    ClientConnectionManager(String ip, Integer port, ClientServerPriorityQueue serverQueue, DataManagerReader dataRead) {
        //TODO create connection acceptor thread


        super();
        this.que = serverQueue;
        this.dataRead = dataRead;


    }

    //TODO implement read
    public Object read(){
        return null;
    }

    //TODO implement write
    public void write(Object value){
    }
}
