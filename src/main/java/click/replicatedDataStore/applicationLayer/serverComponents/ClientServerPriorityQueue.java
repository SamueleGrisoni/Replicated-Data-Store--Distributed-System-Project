package click.replicatedDataStore.applicationLayer.serverComponents;

import click.replicatedDataStore.applicationLayer.Server;
import click.replicatedDataStore.dataStructures.ClientWrite;
import click.replicatedDataStore.dataStructures.ClockedData;

import java.util.PriorityQueue;

public class ClientServerPriorityQueue {
    private final Server server;
    private final PriorityQueue<ClockedData> clientQueue;
    private final PriorityQueue<ClockedData> serversQueue;
    private final Object newDataLock;

    public ClientServerPriorityQueue(Server server) {
        this.server = server;
        this.clientQueue = new PriorityQueue<>();
        this.serversQueue = new PriorityQueue<>();
        this.newDataLock = new Object();
    }

    //return true if the operation was successful
    public synchronized boolean addClientData(ClientWrite clientData){
        //todo
        return false;
    }

    public void addServerData(ClockedData serverData){
        //todo
    }

    public ClockedData popData(){
        //todo: remember to check if the data is from the client or the server. Update lock accordingly
        return null;
    }
}
