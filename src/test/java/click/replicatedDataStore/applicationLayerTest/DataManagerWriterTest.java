package click.replicatedDataStore.applicationLayerTest;

import click.replicatedDataStore.TestUtils;
import click.replicatedDataStore.applicationLayer.Server;
import click.replicatedDataStore.applicationLayer.serverComponents.ServerDataSynchronizer;
import click.replicatedDataStore.dataStructures.*;
import click.replicatedDataStore.utils.Key;
import click.replicatedDataStore.utils.configs.LoadedLocalServerConfig;
import click.replicatedDataStore.utils.configs.ServerConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

//todo
//with the new server implementation it probably would be possible to create a separated test folder by delaying the start of the server
public class DataManagerWriterTest {
    private Server mockServer;
    private Server mockServer2;
    private ServerDataSynchronizer mockServerDataSynchronizer1;
    private ServerDataSynchronizer mockServerDataSynchronizer2;
    Map<Integer, Pair<String, ServerPorts>> addresses = Map.of(
            0, new Pair<>("localhost", new ServerPorts(TestUtils.getPort(), TestUtils.getPort())),
            1, new Pair<>("localhost", new ServerPorts(TestUtils.getPort(), TestUtils.getPort())));

    @Before
    public void setUp() {
        LoadedLocalServerConfig config0 = new LoadedLocalServerConfig("Server0", "localhost", addresses.get(0).second(),
                true, true, Set.of(0,1), Set.of(0,1));
        LoadedLocalServerConfig config1 = new LoadedLocalServerConfig("Server1", "localhost", addresses.get(1).second(),
                true, true, Set.of(0,1), Set.of(0,1));
        mockServer = new Server("Server0", 0, addresses, config0);
        mockServer2 = new Server("Server1", 1, addresses, config1);
        Field serverDataSynchronizerField = getField(Server.class, "serverDataSynchronizer");
        try {
            mockServerDataSynchronizer1 = (ServerDataSynchronizer) serverDataSynchronizerField.get(mockServer);
            mockServerDataSynchronizer2 = (ServerDataSynchronizer) serverDataSynchronizerField.get(mockServer2);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        mockServer.start();
        mockServer2.start();
    }

    @After
    public void tearDown() {
        mockServer.stopServer();
        mockServer2.stopServer();
        List<File> dataFolders = new LinkedList<>();
        for (int i = 0; i < addresses.size(); i++) {
            dataFolders.add(new File(ServerConfig.getGlobalFolderPath() + ServerConfig.SERVER_DATA_FOLDER_NAME + i + File.separator));
        }
        dataFolders.forEach(this::deleteDataFolder);
        // Verify that the data folders have been deleted.
        assertEquals(0, dataFolders.stream().filter(File::exists).count());
    }

    private void deleteDataFolder(File dataFolder) {
        System.out.println("Deleting data folder: " + dataFolder);
        File[] files = dataFolder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDataFolder(file);
                } else {
                    if (!file.delete()) {
                        System.out.println("Failed to delete file: " + file + " retrying...");
                        deleteDataFolder(file);
                    }
                }
            }
        }
        if (!dataFolder.delete()) {
            System.out.println("Failed to delete folder: " + dataFolder + "retrying...");
        }
    }

    @Test
    public void addClientData() throws InterruptedException {
        TestKey key = new TestKey("key1");
        ClientWrite clientWrite = new ClientWrite(key, "value1");
        mockServer.addClientData(clientWrite);
        // Wait for the writer thread to process the data
        Thread.sleep(100);
        assertEquals(1, mockServerDataSynchronizer1.getPrimaryIndex().size());
        assertEquals("value1", mockServerDataSynchronizer1.getPrimaryIndex().get(key));

        TestKey key2 = new TestKey("key2");
        ClientWrite clientWrite2 = new ClientWrite(key2, "value2");
        mockServer.addClientData(clientWrite2);
        // Wait for the writer thread to process the data
        Thread.sleep(100);
        assertEquals(2, mockServerDataSynchronizer1.getPrimaryIndex().size());
        assertEquals("value2", mockServerDataSynchronizer1.getPrimaryIndex().get(key2));
        Thread.sleep(1000);
    }

    @Test
    public void addServerData() throws InterruptedException {
        TestKey key = new TestKey("key1");
        VectorClock otherServerClock = new VectorClock("OtherServer", 2, 1);
        otherServerClock.incrementSelfClock(); // [0, 1]
        ClockedData serverData = new ClockedData(otherServerClock, key, "value1");
        mockServer.addServerData(List.of(serverData));
        // Wait for the writer thread to process the data
        Thread.sleep(100);
        assertEquals(1, mockServerDataSynchronizer1.getPrimaryIndex().size());
        assertEquals("value1", mockServerDataSynchronizer1.getPrimaryIndex().get(key));
    }

    @Test
    public void testClientUpdateReceivedOnOtherServer() throws InterruptedException {
        TestKey key = new TestKey("key1");
        ClientWrite clientWrite = new ClientWrite(key, "value1");
        mockServer.addClientData(clientWrite);
        // Wait for the writer thread to process the data
        Thread.sleep(100);
        assertEquals(1, mockServerDataSynchronizer1.getPrimaryIndex().size());
        assertEquals("value1", mockServerDataSynchronizer1.getPrimaryIndex().get(key));
        assertEquals(1, mockServerDataSynchronizer2.getPrimaryIndex().size());
        assertEquals("value1", mockServerDataSynchronizer2.getPrimaryIndex().get(key));

        ClientWrite clientUpdate = new ClientWrite(key, "updateValue1");
        mockServer.addClientData(clientUpdate);
        // Wait for the writer thread to process the data
        Thread.sleep(100);
        assertEquals(1, mockServerDataSynchronizer1.getPrimaryIndex().size());
        assertEquals("updateValue1", mockServerDataSynchronizer1.getPrimaryIndex().get(key));
        assertEquals(1, mockServerDataSynchronizer2.getPrimaryIndex().size());
        assertEquals("updateValue1", mockServerDataSynchronizer2.getPrimaryIndex().get(key));

        TestKey otherClientKey = new TestKey("OtherClientKey");
        ClientWrite otherClientWrite = new ClientWrite(otherClientKey, "value2");
        mockServer2.addClientData(otherClientWrite);
        // Wait for the writer thread to process the data
        Thread.sleep(100);
        assertEquals(2, mockServerDataSynchronizer2.getPrimaryIndex().size());
        assertEquals("value2", mockServerDataSynchronizer2.getPrimaryIndex().get(otherClientKey));
        assertEquals(2, mockServerDataSynchronizer1.getPrimaryIndex().size());
        assertEquals("value2", mockServerDataSynchronizer1.getPrimaryIndex().get(otherClientKey));
    }

    @Test
    public void testSecondaryWrite() {
        injectNUMBER_OF_WRITE_SECONDARY_INTERVAL(2);

        TestKey key = new TestKey("key1");
        ClientWrite clientWrite = new ClientWrite(key, "value1");
        mockServer.addClientData(clientWrite);
        TestKey key2 = new TestKey("key2");
        ClientWrite clientWrite2 = new ClientWrite(key2, "value2");
        mockServer.addClientData(clientWrite2);
        TestKey key3 = new TestKey("key3");
        ClientWrite clientWrite3 = new ClientWrite(key3, "value3");
        mockServer.addClientData(clientWrite3);

        // Wait for the writer thread to process the data
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        assertEquals(3, mockServerDataSynchronizer1.getPrimaryIndex().size());
        assertEquals(1, mockServerDataSynchronizer1.getSecondaryIndex().size());
        //Because i'm saving every two entries, [2,0] is the expected and only vector clock in secondary
        VectorClock expectedVC = createComparableVectorClock("otherServer", 2, 0, 2); // [2,0]
        assertTrue(mockServerDataSynchronizer1.getSecondaryIndex().containsKey(expectedVC));
        assertEquals(1, mockServerDataSynchronizer1.getSecondaryIndex().size());
    }

    private record TestKey(String keyValue) implements Key {

        @Override
        public String toString() {
            return keyValue;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            DataManagerWriterTest.TestKey testKey = (DataManagerWriterTest.TestKey) o;
            return testKey.keyValue().equals(keyValue);
        }

        @Override
        public int hashCode() {
            return keyValue.hashCode();
        }
    }

    private void injectNUMBER_OF_WRITE_SECONDARY_INTERVAL(int value) {
        System.out.println("Original NUMBER_OF_WRITE_BETWEEN_SECONDARY_INDEX_UPDATE: " + ServerConfig.NUMBER_OF_WRITE_BETWEEN_SECONDARY_INDEX_UPDATE);
        Field NUMBER_OF_WRITE_SECONDARY = null;
        try {
            NUMBER_OF_WRITE_SECONDARY = ServerConfig.class.getDeclaredField("NUMBER_OF_WRITE_BETWEEN_SECONDARY_INDEX_UPDATE");
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        //remove final modifier
        NUMBER_OF_WRITE_SECONDARY.setAccessible(true);
        try {
            NUMBER_OF_WRITE_SECONDARY.setInt(null, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Injected NUMBER_OF_WRITE_BETWEEN_SECONDARY_INDEX_UPDATE: " + ServerConfig.NUMBER_OF_WRITE_BETWEEN_SECONDARY_INDEX_UPDATE);
    }

    private VectorClock createComparableVectorClock(String serverName, int serverNumber, int serverID, int offset) {
        VectorClock vectorClock = new VectorClock(serverName, serverNumber, serverID);
        for (int i = 0; i < offset; i++) {
            vectorClock.incrementSelfClock();
        }
        return vectorClock;
    }

    private Field getField(Class<?> clazz, String fieldName) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }
}
