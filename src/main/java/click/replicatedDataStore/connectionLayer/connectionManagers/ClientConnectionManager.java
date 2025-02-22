package click.replicatedDataStore.connectionLayer.connectionManagers;

import click.replicatedDataStore.connectionLayer.connectionThreads.ConnectedClients;

import java.util.HashMap;
import java.util.List;

public class ClientConnectionManager extends ConnectionManager {
    private HashMap<String, Runnable> routingTable;
    private List<ConnectedClients> connectedClientsList;
    private final Object que; //TODO make it the server queue
    private final Object dataRead; //TODO make it the server dataRead component

    ClientConnectionManager(String ip, Integer port) {
        this.routingTable = new HashMap<>();
        //TODO create connection acceptor thread

        que = new Object(); //TODO make it the server queue
        dataRead = new Object(); //TODO make it the server dataRead component
    }

    public HashMap<String, Runnable> getRoutes() {
        return routingTable;
    }

    //TODO implement read
    public Object read(){
        return null;
    }

    //TODO implement write
    public void write(Object value){
    }
}
