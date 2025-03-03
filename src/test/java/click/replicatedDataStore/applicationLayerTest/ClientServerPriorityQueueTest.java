package click.replicatedDataStore.applicationLayerTest;

import click.replicatedDataStore.applicationLayer.Server;
import click.replicatedDataStore.applicationLayer.serverComponents.ClientServerPriorityQueue;
import click.replicatedDataStore.dataStructures.ClientWrite;
import click.replicatedDataStore.dataStructures.ClockedData;
import click.replicatedDataStore.dataStructures.VectorClock;
import click.replicatedDataStore.utlis.Key;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

public class ClientServerPriorityQueueTest {
    private ClientServerPriorityQueue queue;
    private Server mockServer;

    @Before
    public void setUp() {
        mockServer = new Server(0, 3) {
            //return a vector clock [offset, 0, 0]
            @Override
            public VectorClock getOffsetVectorClock(int offset) {
                VectorClock vc = new VectorClock(3, 0);
                return new VectorClock(vc, offset);
            }
        };
        queue = new ClientServerPriorityQueue(mockServer);
    }

    @Test
    public void testAddClientData() {
        TestKey key = new TestKey("key1");
        ClientWrite clientData = new ClientWrite(key, "value1");
        //Queue is empty (size 0), the offset vector clock should be [1, 0, 0]
        VectorClock compareVC = new VectorClock(3, 0);
        compareVC.incrementSelfClock(); //vc = [1, 0, 0]

        queue.addClientData(clientData);
        ClockedData result = queue.peekData().get(0);

        assertNotNull(result);
        assertEquals(key, result.key());
        assertEquals("value1", result.value());
        assertEquals(compareVC, result.vectorClock());
        queue.popData();
    }

    @Test
    public void testAddServerData() {
        TestKey key = new TestKey("key1");
        VectorClock otherServerVC = createOtherServerVC();

        ClockedData serverData = new ClockedData(otherServerVC, key, "serverValue");
        queue.addServerData(List.of(serverData));
        ClockedData result = queue.peekData().get(0);

        assertNotNull(result);
        assertEquals(key, result.key());
        assertEquals("serverValue", result.value());
        assertEquals(otherServerVC, result.vectorClock());
        queue.popData();
    }

    @Test
    public void testClientDataHasPriority() {
        VectorClock otherServerVC = createOtherServerVC();
        TestKey serverKey = new TestKey("serverKey");
        TestKey clientKey = new TestKey("clientKey");
        ClockedData serverData = new ClockedData(otherServerVC, serverKey, "serverValue");

        queue.addServerData(List.of(serverData));

        ClientWrite clientData = new ClientWrite(clientKey, "clientValue");
        queue.addClientData(clientData);

        //clientData should be popped first
        ClockedData result = queue.peekData().get(0);
        assertNotNull(result);
        assertEquals(clientKey, result.key());
        assertEquals("clientValue", result.value());
        queue.popData();
        result = queue.peekData().get(0);
        assertNotNull(result);
        assertEquals(serverKey, result.key());
        assertEquals("serverValue", result.value());
        queue.popData();
    }

    @Test
    public void testBlockingPopData() throws InterruptedException {
        TestKey key = new TestKey("key1");
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                Thread.sleep(500);
                System.out.println("Adding data");
                queue.addClientData(new ClientWrite(key, "delayedValue"));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        //Queue is empty, popData should block until data is added
        System.out.println("Blocking popData");
        ClockedData result = queue.peekData().get(0);
        assertNotNull(result);
        assertEquals(key, result.key());
        assertEquals("delayedValue", result.value());
        queue.popData();
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.SECONDS);
    }

    //Dummy Key class for testing
    private record TestKey(String keyValue) implements Key {

        @Override
        public String toString() {
            return keyValue;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            ClientServerPriorityQueueTest.TestKey testKey = (ClientServerPriorityQueueTest.TestKey) o;
            return testKey.keyValue().equals(keyValue);
        }

        @Override
        public int hashCode() {
            return keyValue.hashCode();
        }
    }

    private VectorClock createOtherServerVC() {
        VectorClock otherServerVC = new VectorClock(3, 1);
        otherServerVC.incrementSelfClock(); //vc = [0, 1, 0]
        otherServerVC.incrementSelfClock(); //vc = [0, 2, 0]
        return otherServerVC;
    }
}