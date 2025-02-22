package click.replicatedDataStore.applicationLayer.serverComponents.DataManager;

import click.replicatedDataStore.applicationLayer.Server;
import click.replicatedDataStore.applicationLayer.serverComponents.ClientServerPriorityQueue;
import click.replicatedDataStore.utlis.Key;

public class DataManagerWriter extends Thread{
    private final Server server;
    private final ClientServerPriorityQueue clientServerPriorityQueue;
    public DataManagerWriter(Server server, ClientServerPriorityQueue clientServerPriorityQueue) {
        this.server = server;
        this.clientServerPriorityQueue = clientServerPriorityQueue;
    }

    @Override
    public void run() {
        //todo
    }

    private synchronized void write(Key key, Object value) {
        //todo
    }
}
