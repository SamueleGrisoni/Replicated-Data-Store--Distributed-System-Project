package click.replicatedDataStore.applicationLayer.serverComponents;

import click.replicatedDataStore.applicationLayer.Server;
import click.replicatedDataStore.dataStructures.ClientWrite;
import click.replicatedDataStore.dataStructures.ClockedData;
import click.replicatedDataStore.dataStructures.VectorClock;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class ClientServerPriorityQueue {
    private final Server server;
    private final PriorityQueue<ClockedData> clientQueue;
    private final PriorityQueue<List<ClockedData>> serversQueue;
    private final Object lock = new Object();

    public ClientServerPriorityQueue(Server server) {
        this.server = server;
        this.clientQueue = new PriorityQueue<>();
        this.serversQueue = new PriorityQueue<>(Comparator.comparing(list -> list.get(0).vectorClock()));
    }

    //Synchronized because the offset vector clock is calculated based on the current size of the clientQueue and current vector clock
    public boolean addClientData(ClientWrite clientData) {
        //Offset = current size of the clientQueue + this clientData (current size + 1)
        synchronized (lock) {
            VectorClock offsetVectorClock = server.getOffsetVectorClock(clientQueue.size() + 1);
            //System.out.println("Offset vector clock: " + offsetVectorClock);
            ClockedData clockedData = new ClockedData(offsetVectorClock, clientData.key(), clientData.value());
            clientQueue.add(clockedData);
            lock.notify();
        }
        return true;
    }

    public void addServerData(List<ClockedData> serverData) {
        synchronized (lock) {
            serversQueue.add(serverData);
            lock.notify();
        }
    }

    //Prefer user update to server update. If both queues are empty, update the lock so the writerThread requesting pops is blocked
    public List<ClockedData> peekData() {
        synchronized (lock) {
            while (clientQueue.isEmpty() && serversQueue.isEmpty()) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    // Restore interrupted status
                    Thread.currentThread().interrupt();
                }
            }
            return !clientQueue.isEmpty() ? List.of(clientQueue.peek()) : serversQueue.peek();
        }
    }

    public void popData() {
        synchronized (lock) {
            if(!clientQueue.isEmpty()){
                clientQueue.poll();
            }else{
                serversQueue.poll();
            }
        }
    }
}
