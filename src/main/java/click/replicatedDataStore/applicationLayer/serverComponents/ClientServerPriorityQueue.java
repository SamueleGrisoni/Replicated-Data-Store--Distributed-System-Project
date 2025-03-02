package click.replicatedDataStore.applicationLayer.serverComponents;

import click.replicatedDataStore.applicationLayer.Server;
import click.replicatedDataStore.dataStructures.ClientWrite;
import click.replicatedDataStore.dataStructures.ClockedData;
import click.replicatedDataStore.dataStructures.VectorClock;

import java.util.PriorityQueue;

public class ClientServerPriorityQueue {
    private final Server server;
    private final PriorityQueue<ClockedData> clientQueue;
    private final PriorityQueue<ClockedData> serversQueue;
    private final Object lock = new Object();

    public ClientServerPriorityQueue(Server server) {
        this.server = server;
        this.clientQueue = new PriorityQueue<>();
        this.serversQueue = new PriorityQueue<>();
    }

    public void addClientData(ClientWrite clientData){
        //Offset = current size of the clientQueue + this clientData (current size + 1)
        VectorClock offsetVectorClock = server.getOffsetVectorClock(clientQueue.size() + 1);
        ClockedData clockedData = new ClockedData(offsetVectorClock, clientData.key(), clientData.value());
        synchronized (lock){
            clientQueue.add(clockedData);
            lock.notify();
        }
    }

    public void addServerData(ClockedData serverData){
        synchronized (lock){
            serversQueue.add(serverData);
            lock.notify();
        }
    }

    //Prefer user update to server update. If both queues are empty, update the lock so the writerThread requesting pops
    public ClockedData popData() {
        synchronized (lock) {
            while (clientQueue.isEmpty() && serversQueue.isEmpty()) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    // Restore interrupted status
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
            return !clientQueue.isEmpty() ? clientQueue.poll() : serversQueue.poll();
        }
    }
}
