import click.replicatedDataStore.TestUtils;
import click.replicatedDataStore.applicationLayer.Server;
import click.replicatedDataStore.applicationLayerTest.DataManagerWriterTest;
import click.replicatedDataStore.dataStructures.ClientWrite;
import click.replicatedDataStore.dataStructures.Pair;
import click.replicatedDataStore.dataStructures.ServerPorts;
import click.replicatedDataStore.dataStructures.VectorClock;
import click.replicatedDataStore.utils.Key;
import click.replicatedDataStore.utils.configs.ConfigFile;
import click.replicatedDataStore.utils.configs.LoadedConfig;
import click.replicatedDataStore.utils.configs.LoadedLocalServerConfig;
import click.replicatedDataStore.utils.configs.ServerConfig;
import click.replicatedDataStore.utils.serverUtilis.ServerInitializerUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.*;

public class ConcurrentTest {
    Map<Integer, Pair<String, ServerPorts>> addresses = Map.of(
            0, new Pair<>("127.0.0.1", new ServerPorts(TestUtils.getPort(), TestUtils.getPort())),
            1, new Pair<>("127.0.0.1", new ServerPorts(TestUtils.getPort(), TestUtils.getPort())),
            2, new Pair<>("127.0.0.1", new ServerPorts(TestUtils.getPort(), TestUtils.getPort())));
    Server serverA, serverB, serverC;

    @Before
    public void setup() throws InterruptedException {
        deleteDataFolder(new File(ServerConfig.getGlobalFolderPath()));
        LoadedLocalServerConfig config0 = new LoadedLocalServerConfig("testA", "127.0.0.1", addresses.get(0).second(),
                false, false, Set.of(1,2), Set.of(1,2));
        LoadedLocalServerConfig config1 = new LoadedLocalServerConfig("testB", "127.0.0.1", addresses.get(1).second(),
                false, false, Set.of(0,2), Set.of(0,2));
        LoadedLocalServerConfig config2 = new LoadedLocalServerConfig("testC", "127.0.0.1", addresses.get(2).second(),
                false, false, Set.of(0,1), Set.of(0,1));

        Pair<Map<Integer, LoadedLocalServerConfig>, Map<Integer, LoadedConfig>> serverConfigs = new Pair<>(new LinkedHashMap<>(), new LinkedHashMap<>());
        serverConfigs.first().put(0, config0);
        serverConfigs.first().put(1, config1);
        serverConfigs.first().put(2, config2);

        ServerInitializerUtils initializerUtils = new ServerInitializerUtils();
        initializerUtils.setServerConfigs(serverConfigs);
        initializerUtils.setServerIndexToName(Map.of(0, "testA", 1, "testB", 2, "testC"));
        initializerUtils.setServerNameToIndex(Map.of("testA", 0, "testB", 1, "testC", 2));
        initializerUtils.startAllLocalServer();

        serverA = initializerUtils.getLocalServers().get(0).first();
        serverB = initializerUtils.getLocalServers().get(1).first();
        serverC = initializerUtils.getLocalServers().get(2).first();

        Thread.sleep(2000); //Wait for servers to start
    }

    long delayFactor = 3;

    @Test
    public void testConcurrentHistory() throws InterruptedException {
        Thread.sleep(10000); //Wait for servers to start

        serverA.addClientData(new ClientWrite(new TestKey("keyA1"), "valueA1"));
        serverB.addClientData(new ClientWrite(new TestKey("keyB1"), "valueB1"));
        Thread.sleep(delayFactor * (ServerConfig.LIGHT_PUSH_DELAY_MILLIS + ServerConfig.LIGHT_PUSH_RANDOM_DELAY_MILLIS));
        Assert.assertEquals(new TestVectorClock("testA", 3, 0, new int[]{1,1,0}), serverA.getVectorClock());
        Assert.assertEquals(new TestVectorClock("testB", 3, 1, new int[]{1,1,0}), serverB.getVectorClock());
        Assert.assertEquals(new TestVectorClock("testC", 3, 2, new int[]{1,1,0}), serverC.getVectorClock());

        serverA.disconnect();
        serverB.addClientData(new ClientWrite(new TestKey("keyB2"), "valueB2"));
        serverC.addClientData(new ClientWrite(new TestKey("keyC1"), "valueC1"));
        Thread.sleep(delayFactor * (ServerConfig.LIGHT_PUSH_DELAY_MILLIS + ServerConfig.LIGHT_PUSH_RANDOM_DELAY_MILLIS));
        //Disconnected serverA should not have received the update from serverB and C
        Assert.assertEquals(new TestVectorClock("testA", 3, 0, new int[]{1,1,0}), serverA.getVectorClock());
        Assert.assertEquals(new TestVectorClock("testB", 3, 1, new int[]{1,2,1}), serverB.getVectorClock());
        Assert.assertEquals(new TestVectorClock("testC", 3, 2, new int[]{1,2,1}), serverC.getVectorClock());

        //there is now a concurrent history [2,1,0] for 'a' and [1,2,1] for 'b' and 'c'
        serverA.addClientData(new ClientWrite(new TestKey("keyA2"), "valueA2"));
        Thread.sleep(delayFactor * 1000);
        Assert.assertEquals(new TestVectorClock("testA", 3, 0, new int[]{2,1,0}), serverA.getVectorClock());
        Assert.assertEquals(new TestVectorClock("testB", 3, 1, new int[]{1,2,1}), serverB.getVectorClock());
        Assert.assertEquals(new TestVectorClock("testC", 3, 2, new int[]{1,2,1}), serverC.getVectorClock());

        serverA.reconnect();
        Thread.sleep(delayFactor * (ServerConfig.LIGHT_PUSH_DELAY_MILLIS + ServerConfig.LIGHT_PUSH_RANDOM_DELAY_MILLIS));
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

    @After
    public void teardown() {
        serverA.stopServer();
        serverB.stopServer();
        serverC.stopServer();
        deleteDataFolder(new File(ServerConfig.getGlobalFolderPath()));
    }
}
