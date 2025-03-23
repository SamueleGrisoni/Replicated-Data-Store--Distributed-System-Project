package click.replicatedDataStore.applicationLayer.serverComponents;

import click.replicatedDataStore.dataStructures.ClientWrite;
import click.replicatedDataStore.dataStructures.ClockedData;
import click.replicatedDataStore.dataStructures.Pair;
import click.replicatedDataStore.dataStructures.VectorClock;
import click.replicatedDataStore.utils.DataType;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ClientServerPriorityQueue {
    private final ServerDataSynchronizer serverDataSynchronizer;
    private final PriorityQueue<ClockedData> clientQueue;
    private final PriorityQueue<List<ClockedData>> serversQueue;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();

    public ClientServerPriorityQueue(ServerDataSynchronizer serverDataSynchronizer) {
        this.serverDataSynchronizer = serverDataSynchronizer;
        this.clientQueue = new PriorityQueue<>();
        this.serversQueue = new PriorityQueue<>(Comparator.comparing(list -> list.get(0).vectorClock()));
    }

    public void lockQueue() {
        lock.lock();
    }

    public void unlockQueue() {
        lock.unlock();
    }

    //Synchronized because the offset vector clock is calculated based on the current size of the clientQueue and current vector clock
    public boolean addClientData(ClientWrite clientData) {
        //Offset = current size of the clientQueue + this clientData (current size + 1)
        lock.lock();
        try {
            VectorClock offsetVectorClock = serverDataSynchronizer.getOffsetVectorClock(clientQueue.size() + 1);
            ClockedData clockedData = new ClockedData(offsetVectorClock, clientData.key(), clientData.value());
            clientQueue.add(clockedData);
            //Notify the writer thread that there is data to be written
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
        return true;
    }

    public void addServerData(List<ClockedData> serverData) {
        lock.lock();
        try {
            serversQueue.add(serverData);
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    //Prefer user update to server update. If both queues are empty, update the lock so the writerThread requesting pops is blocked
    public Pair<DataType, List<ClockedData>> pollData() {
        while (clientQueue.isEmpty() && serversQueue.isEmpty()) {
            try {
                notEmpty.await();
            } catch (InterruptedException e) {
                // Restore interrupted status
                Thread.currentThread().interrupt();
            }
        }
        return !clientQueue.isEmpty() ? new Pair<>(DataType.CLIENT, List.of(clientQueue.poll())) : new Pair<>(DataType.SERVER, serversQueue.poll());
    }

    public Condition getNotEmptyCondition(){
        return notEmpty;
    }
}
