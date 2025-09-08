import click.replicatedDataStore.TestUtils;
import click.replicatedDataStore.applicationLayer.Server;
import click.replicatedDataStore.applicationLayerTest.DataManagerWriterTest;
import click.replicatedDataStore.dataStructures.ClientWrite;
import click.replicatedDataStore.dataStructures.Pair;
import click.replicatedDataStore.dataStructures.ServerPorts;
import click.replicatedDataStore.dataStructures.VectorClock;
import click.replicatedDataStore.utils.Key;
import click.replicatedDataStore.utils.configs.LoadedLocalServerConfig;
import click.replicatedDataStore.utils.configs.ServerConfig;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

public class ConcurrentTest {
    Map<Integer, Pair<String, ServerPorts>> addresses = Map.of(
            0, new Pair<>("localhost", new ServerPorts(TestUtils.getPort(), TestUtils.getPort())),
            1, new Pair<>("localhost", new ServerPorts(TestUtils.getPort(), TestUtils.getPort())),
            2, new Pair<>("localhost", new ServerPorts(TestUtils.getPort(), TestUtils.getPort())));
    Server serverA, serverB, serverC;

    @Before
    public void setup() throws InterruptedException {
        deleteDataFolder(new File(ServerConfig.getGlobalFolderPath()));
        LoadedLocalServerConfig config0 = new LoadedLocalServerConfig("Server0", "localhost", addresses.get(0).second(),
                true, true, Set.of(1,2), Set.of(1,2));
        LoadedLocalServerConfig config1 = new LoadedLocalServerConfig("Server1", "localhost", addresses.get(1).second(),
                true, true, Set.of(0,2), Set.of(0,2));
        LoadedLocalServerConfig config2 = new LoadedLocalServerConfig("Server2", "localhost", addresses.get(2).second(),
                true, true, Set.of(0,1), Set.of(0,1));
        serverA = new Server("testA", 0, addresses, config0);
        serverB = new Server("testB", 1, addresses, config1);
        serverC = new Server("testC", 2, addresses, config2);
        serverA.start();
        serverB.start();
        serverC.start();
        Thread.sleep(2000); //Wait for servers to start
    }

    @Test
    public void testConcurrentHistory() throws InterruptedException {
        serverA.addClientData(new ClientWrite(new TestKey("keyA1"), "valueA1"));
        serverB.addClientData(new ClientWrite(new TestKey("keyB1"), "valueB1"));
        Thread.sleep(1000);
        Assert.assertEquals(new TestVectorClock("testA", 3, 0, new int[]{1,1,0}), serverA.getVectorClock());
        Assert.assertEquals(new TestVectorClock("testB", 3, 1, new int[]{1,1,0}), serverB.getVectorClock());
        Assert.assertEquals(new TestVectorClock("testC", 3, 2, new int[]{1,1,0}), serverC.getVectorClock());

        serverA.disconnect();
        serverB.addClientData(new ClientWrite(new TestKey("keyB2"), "valueB2"));
        serverC.addClientData(new ClientWrite(new TestKey("keyC1"), "valueC1"));
        Thread.sleep(3000);
        //Disconnected serverA should not have received the update from serverB and C
        Assert.assertEquals(new TestVectorClock("testA", 3, 0, new int[]{1,1,0}), serverA.getVectorClock());
        Assert.assertEquals(new TestVectorClock("testB", 3, 1, new int[]{1,2,1}), serverB.getVectorClock());
        Assert.assertEquals(new TestVectorClock("testC", 3, 2, new int[]{1,2,1}), serverC.getVectorClock());

        //there is now a concurrent history [2,1,0] for 'a' and [1,2,1] for 'b' and 'c'
        serverA.addClientData(new ClientWrite(new TestKey("keyA2"), "valueA2"));
        Thread.sleep(1000);
        Assert.assertEquals(new TestVectorClock("testA", 3, 0, new int[]{2,1,0}), serverA.getVectorClock());
        Assert.assertEquals(new TestVectorClock("testB", 3, 1, new int[]{1,2,1}), serverB.getVectorClock());
        Assert.assertEquals(new TestVectorClock("testC", 3, 2, new int[]{1,2,1}), serverC.getVectorClock());

        serverA.reconnect();
        Thread.sleep(3000);
        //After reconnect all servers should have merged the concurrent history to [2,2,1]
        Assert.assertEquals(new TestVectorClock("testA", 3, 0, new int[]{2,2,1}), serverA.getVectorClock());
        Assert.assertEquals(new TestVectorClock("testB", 3, 1, new int[]{2,2,1}), serverB.getVectorClock());
        Assert.assertEquals(new TestVectorClock("testC", 3, 2, new int[]{2,2,1}), serverC.getVectorClock());
    }

    private void deleteDataFolder(File dataFolder){
        System.out.println("Deleting data folder: " + dataFolder.getAbsolutePath());
        if (dataFolder.exists()) {
            for (File file : dataFolder.listFiles()) {
                if (!file.isDirectory()) {
                    if (!file.delete()) {
                        System.err.println("Failed to delete file: " + file.getAbsolutePath());
                    } else {
                        System.out.println("Deleted file: " + file.getAbsolutePath());
                    }
                }else{
                    deleteDataFolder(file);
                }
            }
        } else {
            System.out.println("Data folder does not exist: " + dataFolder.getAbsolutePath());
        }
    }

    private record TestKey(String keyValue) implements Key {

        @Override
        public String toString() {
            return keyValue;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            TestKey testKey = (TestKey) obj;
            return keyValue.equals(testKey.keyValue);
        }

        @Override
        public int hashCode() {
            return keyValue.hashCode();
        }
    }

    private class TestVectorClock extends VectorClock{
        public TestVectorClock(String serverName, int serverNumber, int serverIndex, int[] clock) {
            super(serverName, serverNumber, serverIndex);
            if(clock.length != serverNumber) {
                throw new IllegalArgumentException("Clock length must match server number");
            }
            System.arraycopy(clock, 0, this.clock, 0, clock.length);
        }

        //N.B. This do not check name or serverIndex it is just used for testing equality of the clock array
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || !(obj instanceof VectorClock)) return false;
            VectorClock that = (VectorClock) obj;
            return Arrays.equals(this.clock, that.getClock());
        }
    }
}
