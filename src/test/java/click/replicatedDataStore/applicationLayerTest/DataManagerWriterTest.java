package click.replicatedDataStore.applicationLayerTest;

import click.replicatedDataStore.TestUtils;
import click.replicatedDataStore.applicationLayer.Server;
import click.replicatedDataStore.applicationLayer.serverComponents.ServerDataSynchronizer;
import click.replicatedDataStore.dataStructures.*;
import click.replicatedDataStore.utlis.Key;
import click.replicatedDataStore.utlis.configs.ServerConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

//todo
//with the new server implementation it probably would be possible to create a separated test folder by delaying the start of the server
//todo 2: because the ServerInitializerUtils is not initialize the map serverIdToIndex is null. A Null Pointer Exception is thrown and the vectorClock is print with -1
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
        mockServer = new Server(0, addresses);
        mockServer2 = new Server(1, addresses);
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
        VectorClock otherServerClock = new VectorClock(2, 1);
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
        VectorClock expectedVC = createComparableVectorClock(2, 0, 2); // [2,0]
        assertTrue(mockServerDataSynchronizer1.getSecondaryIndex().containsKey(expectedVC));
        assertEquals(1, mockServerDataSynchronizer1.getSecondaryIndex().size());
    }

    @Test
    public void testSecondaryUpdate() {
        injectNUMBER_OF_WRITE_SECONDARY_INTERVAL(2);

        TestKey keyA = new TestKey("A");
        ClientWrite clientWrite = new ClientWrite(keyA, "value1");
        mockServer.addClientData(clientWrite);
        TestKey keyB = new TestKey("B");
        ClientWrite clientWrite2 = new ClientWrite(keyB, "value2");
        mockServer.addClientData(clientWrite2);
        TestKey keyC = new TestKey("C");
        ClientWrite clientWrite3 = new ClientWrite(keyC, "value3");
        mockServer.addClientData(clientWrite3);
        TestKey keyD = new TestKey("D");
        ClientWrite clientWrite4 = new ClientWrite(keyD, "value4");
        mockServer.addClientData(clientWrite4);
        TestKey keyE = new TestKey("E");
        ClientWrite clientWrite5 = new ClientWrite(keyE, "value5");
        mockServer.addClientData(clientWrite5);

        // Wait for the writer thread to process the data
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        assertEquals(5, mockServerDataSynchronizer1.getPrimaryIndex().size());
        assertEquals(2, mockServerDataSynchronizer1.getSecondaryIndex().size());
        assertTrue(mockServerDataSynchronizer1.getSecondaryIndex().values().stream().anyMatch(key -> key.equals(keyB)));
        assertTrue(mockServerDataSynchronizer1.getSecondaryIndex().values().stream().anyMatch(key -> key.equals(keyD)));
        System.out.println("Secondary Index:");
        for (Map.Entry<VectorClock, Key> entry : mockServerDataSynchronizer1.getSecondaryIndex().entrySet()) {
            System.out.println("VectorClock: " + entry.getKey() + " Key: " + entry.getValue());
        }
        ClientWrite updateClientWrite = new ClientWrite(keyB, "value2.1");
        mockServer.addClientData(updateClientWrite);
        // Wait for the writer thread to process the data
        try {
            Thread.sleep(150);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        assertEquals(5, mockServerDataSynchronizer1.getPrimaryIndex().size());
        //I should have the 2 "old" vector clocks and the new one because this is the 6th write.
        //The oldest vector clock [2,0] should now point to the keyC, because the newestVC clock [6,0] should point to keyB
        System.out.println("Secondary Index after 1° update:");
        for (Map.Entry<VectorClock, Key> entry : mockServerDataSynchronizer1.getSecondaryIndex().entrySet()) {
            System.out.println("VectorClock: " + entry.getKey() + " Key: " + entry.getValue());
        }
        assertEquals(3, mockServerDataSynchronizer1.getSecondaryIndex().size());
        assertTrue(mockServerDataSynchronizer1.getSecondaryIndex().values().stream().anyMatch(key -> key.equals(keyB)));
        assertTrue(mockServerDataSynchronizer1.getSecondaryIndex().values().stream().anyMatch(key -> key.equals(keyC)));
        assertTrue(mockServerDataSynchronizer1.getSecondaryIndex().values().stream().anyMatch(key -> key.equals(keyD)));
        VectorClock newestVC = createComparableVectorClock(2, 0, 6); // [6,0]
        assertTrue(mockServerDataSynchronizer1.getSecondaryIndex().containsKey(newestVC));
        assertEquals(keyB, mockServerDataSynchronizer1.getSecondaryIndex().get(newestVC));
        VectorClock oldestVC = createComparableVectorClock(2, 0, 2); // [2,0]
        assertTrue(mockServerDataSynchronizer1.getSecondaryIndex().containsKey(oldestVC));
        assertEquals(keyC, mockServerDataSynchronizer1.getSecondaryIndex().get(oldestVC));
        VectorClock secondOldestVC = createComparableVectorClock(2, 0, 4); // [4,0]
        assertTrue(mockServerDataSynchronizer1.getSecondaryIndex().containsKey(secondOldestVC));
        //---------------------------------------------
        TestKey keyF = new TestKey("F");
        ClientWrite clientWrite6 = new ClientWrite(keyF, "value6");
        mockServer.addClientData(clientWrite6);
        ClientWrite updateClientWrite2 = new ClientWrite(keyB, "value2.2");
        mockServer.addClientData(updateClientWrite2);
        try {
            Thread.sleep(150);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        //The secondary index should now contain 4 entry. The (old) newestVC should point to keyF, while the (new) newestVC should point to keyB
        System.out.println("Secondary Index after 2° update:");
        for (Map.Entry<VectorClock, Key> entry : mockServerDataSynchronizer1.getSecondaryIndex().entrySet()) {
            System.out.println("VectorClock: " + entry.getKey() + " Key: " + entry.getValue());
        }
        assertEquals(6, mockServerDataSynchronizer1.getPrimaryIndex().size());
        assertEquals(4, mockServerDataSynchronizer1.getSecondaryIndex().size());
        assertTrue(mockServerDataSynchronizer1.getSecondaryIndex().values().stream().anyMatch(key -> key.equals(keyC)));
        assertTrue(mockServerDataSynchronizer1.getSecondaryIndex().values().stream().anyMatch(key -> key.equals(keyD)));
        assertTrue(mockServerDataSynchronizer1.getSecondaryIndex().values().stream().anyMatch(key -> key.equals(keyF)));
        assertTrue(mockServerDataSynchronizer1.getSecondaryIndex().values().stream().anyMatch(key -> key.equals(keyB)));
        VectorClock newestVC2 = createComparableVectorClock(2, 0, 8); // [8,0]
        assertTrue(mockServerDataSynchronizer1.getSecondaryIndex().containsKey(newestVC2));
        assertEquals(keyB, mockServerDataSynchronizer1.getSecondaryIndex().get(newestVC2));
        assertEquals(keyF, mockServerDataSynchronizer1.getSecondaryIndex().get(newestVC));
        //---------------------------------------------
        TestKey keyG = new TestKey("G");
        ClientWrite clientWrite7 = new ClientWrite(keyG, "value7");
        mockServer.addClientData(clientWrite7);
        ClientWrite updateClientWrite3 = new ClientWrite(keyC, "value3.1");
        mockServer.addClientData(updateClientWrite3);
        try {
            Thread.sleep(150);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        //The secondary index should now contain entry 4. The oldestVC should point to keyD, but an entry with VC [4,0] should already exist => the oldestVC should be removed
        assertEquals(7, mockServerDataSynchronizer1.getPrimaryIndex().size());
        assertEquals(4, mockServerDataSynchronizer1.getSecondaryIndex().size());
        System.out.println("Secondary Index after 3° update:");
        for (Map.Entry<VectorClock, Key> entry : mockServerDataSynchronizer1.getSecondaryIndex().entrySet()) {
            System.out.println("VectorClock: " + entry.getKey() + " Key: " + entry.getValue());
        }
        assertTrue(mockServerDataSynchronizer1.getSecondaryIndex().values().stream().anyMatch(key -> key.equals(keyD)));
        assertTrue(mockServerDataSynchronizer1.getSecondaryIndex().values().stream().anyMatch(key -> key.equals(keyF)));
        assertTrue(mockServerDataSynchronizer1.getSecondaryIndex().values().stream().anyMatch(key -> key.equals(keyB)));
        assertTrue(mockServerDataSynchronizer1.getSecondaryIndex().values().stream().anyMatch(key -> key.equals(keyC)));
        assertFalse(mockServerDataSynchronizer1.getSecondaryIndex().containsKey(oldestVC));
    }

    @Test
    public void testSecondaryUpdateWithKeyLastInPrimaryIndex() {
        injectNUMBER_OF_WRITE_SECONDARY_INTERVAL(1);

        TestKey key = new TestKey("keyLast");
        ClientWrite initialWrite = new ClientWrite(key, "initialValue");
        mockServer.addClientData(initialWrite);

        // Wait for the writer thread to process the data.
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // Now update the same key.
        ClientWrite updateWrite = new ClientWrite(key, "updatedValue");
        mockServer.addClientData(updateWrite);

        // Wait for the writer thread to process the update.
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // Verify that the primary index has one entry with the updated value.
        assertEquals(1, mockServerDataSynchronizer1.getPrimaryIndex().size());
        assertEquals("updatedValue", mockServerDataSynchronizer1.getPrimaryIndex().get(key));

        // Verify that the secondary index does not have any mapping with a null key.
        // The expectation is that if computeNewPossibleKey returns null (because the key is last),
        // the old mapping should be removed rather than replaced with a null value.
        // The only entry in the secondary index should be the updated key.
        assertEquals(1, mockServerDataSynchronizer1.getSecondaryIndex().size());
        for (Key secondaryKey : mockServerDataSynchronizer1.getSecondaryIndex().values()) {
            assertNotNull("Secondary index should not contain null keys", secondaryKey);
            assertEquals("Secondary index should map to the updated key", key, secondaryKey);
        }
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

    private VectorClock createComparableVectorClock(int serverNumber, int serverID, int offset) {
        VectorClock vectorClock = new VectorClock(serverNumber, serverID);
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
