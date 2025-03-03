package click.replicatedDataStore.applicationLayerTest;

import click.replicatedDataStore.applicationLayer.Server;
import click.replicatedDataStore.applicationLayer.serverComponents.Persist;
import click.replicatedDataStore.dataStructures.ClientWrite;
import click.replicatedDataStore.dataStructures.ClockedData;
import click.replicatedDataStore.dataStructures.VectorClock;
import click.replicatedDataStore.utlis.ServerConfig;
import click.replicatedDataStore.utlis.Key;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class DataManagerWriterTest {
    private Server mockServer;
    private File persistFolder;
    private Persist mockPersist;

    @Before
    public void setUp() {
        mockServer = new Server(0, 2);
        try {
            mockPersist = persistSetUp();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //Inject the mock persist object into the server
        Field persistField = null;
        try {
             persistField = Server.class.getDeclaredField("persist");
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        persistField.setAccessible(true);
        try {
            persistField.set(mockServer, mockPersist);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @After
    public void tearDown() {
        if (persistFolder.exists()) {
            File[] files = persistFolder.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
            persistFolder.delete();
        }
        //check if the dataFolder is deleted (if it is deleted also the files are deleted)
        Assert.assertFalse(persistFolder.exists());
    }

    @Test
    public void addClientData() throws InterruptedException {
        TestKey key = new TestKey("key1");
        ClientWrite clientWrite = new ClientWrite(key, "value1");
        mockServer.addClientData(clientWrite);
        // Wait for the writer thread to process the data
        Thread.sleep(100);
        assertEquals(1, mockServer.getPrimaryIndex().size());
        assertEquals("value1", mockServer.getPrimaryIndex().get(key));
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
        assertEquals(1, mockServer.getPrimaryIndex().size());
        assertEquals("value1", mockServer.getPrimaryIndex().get(key));
    }

    @Test
    public void testSecondaryWrite(){
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
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        assertEquals(3, mockServer.getPrimaryIndex().size());
        assertEquals(1, mockServer.getSecondaryIndex().size());
        //Because i'm saving every two entries, [2,0] is the expected and only vector clock in secondary
        VectorClock expectedVC = createComparableVectorClock(2, 0, 2); // [2,0]
        assertTrue(mockServer.getSecondaryIndex().containsKey(expectedVC));
        assertEquals(1, mockServer.getSecondaryIndex().size());
    }

    @Test
    public void testSecondaryUpdate(){
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
        assertEquals(5, mockServer.getPrimaryIndex().size());
        assertEquals(2, mockServer.getSecondaryIndex().size());
        assertTrue(mockServer.getSecondaryIndex().values().stream().anyMatch(key -> key.equals(keyB)));
        assertTrue(mockServer.getSecondaryIndex().values().stream().anyMatch(key -> key.equals(keyD)));
        System.out.println("Secondary Index:");
        for(Map.Entry<VectorClock, Key> entry : mockServer.getSecondaryIndex().entrySet()){
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
        assertEquals(5, mockServer.getPrimaryIndex().size());
        //I should have the 2 "old" vector clocks and the new one because this is the 6th write.
        //The oldest vector clock [2,0] should now point to the keyC, because the newestVC clock [6,0] should point to keyB
        System.out.println("Secondary Index after 1° update:");
        for(Map.Entry<VectorClock, Key> entry : mockServer.getSecondaryIndex().entrySet()){
            System.out.println("VectorClock: " + entry.getKey() + " Key: " + entry.getValue());
        }
        assertEquals(3, mockServer.getSecondaryIndex().size());
        assertTrue(mockServer.getSecondaryIndex().values().stream().anyMatch(key -> key.equals(keyB)));
        assertTrue(mockServer.getSecondaryIndex().values().stream().anyMatch(key -> key.equals(keyC)));
        assertTrue(mockServer.getSecondaryIndex().values().stream().anyMatch(key -> key.equals(keyD)));
        VectorClock newestVC = createComparableVectorClock(2, 0, 6); // [6,0]
        assertTrue(mockServer.getSecondaryIndex().containsKey(newestVC));
        assertEquals(keyB, mockServer.getSecondaryIndex().get(newestVC));
        VectorClock oldestVC = createComparableVectorClock(2, 0, 2); // [2,0]
        assertTrue(mockServer.getSecondaryIndex().containsKey(oldestVC));
        assertEquals(keyC, mockServer.getSecondaryIndex().get(oldestVC));
        VectorClock secondOldestVC = createComparableVectorClock(2, 0, 4); // [4,0]
        assertTrue(mockServer.getSecondaryIndex().containsKey(secondOldestVC));
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
        for(Map.Entry<VectorClock, Key> entry : mockServer.getSecondaryIndex().entrySet()){
            System.out.println("VectorClock: " + entry.getKey() + " Key: " + entry.getValue());
        }
        assertEquals(6, mockServer.getPrimaryIndex().size());
        assertEquals(4, mockServer.getSecondaryIndex().size());
        assertTrue(mockServer.getSecondaryIndex().values().stream().anyMatch(key -> key.equals(keyC)));
        assertTrue(mockServer.getSecondaryIndex().values().stream().anyMatch(key -> key.equals(keyD)));
        assertTrue(mockServer.getSecondaryIndex().values().stream().anyMatch(key -> key.equals(keyF)));
        assertTrue(mockServer.getSecondaryIndex().values().stream().anyMatch(key -> key.equals(keyB)));
        VectorClock newestVC2 = createComparableVectorClock(2, 0, 8); // [8,0]
        assertTrue(mockServer.getSecondaryIndex().containsKey(newestVC2));
        assertEquals(keyB, mockServer.getSecondaryIndex().get(newestVC2));
        assertEquals(keyF, mockServer.getSecondaryIndex().get(newestVC));
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
        assertEquals(7, mockServer.getPrimaryIndex().size());
        assertEquals(4, mockServer.getSecondaryIndex().size());
        System.out.println("Secondary Index after 3° update:");
        for(Map.Entry<VectorClock, Key> entry : mockServer.getSecondaryIndex().entrySet()){
            System.out.println("VectorClock: " + entry.getKey() + " Key: " + entry.getValue());
        }
        assertTrue(mockServer.getSecondaryIndex().values().stream().anyMatch(key -> key.equals(keyD)));
        assertTrue(mockServer.getSecondaryIndex().values().stream().anyMatch(key -> key.equals(keyF)));
        assertTrue(mockServer.getSecondaryIndex().values().stream().anyMatch(key -> key.equals(keyB)));
        assertTrue(mockServer.getSecondaryIndex().values().stream().anyMatch(key -> key.equals(keyC)));
        assertFalse(mockServer.getSecondaryIndex().containsKey(oldestVC));
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
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // Verify that the primary index has one entry with the updated value.
        assertEquals(1, mockServer.getPrimaryIndex().size());
        assertEquals("updatedValue", mockServer.getPrimaryIndex().get(key));

        // Verify that the secondary index does not have any mapping with a null key.
        // The expectation is that if computeNewPossibleKey returns null (because the key is last),
        // the old mapping should be removed rather than replaced with a null value.
        // The only entry in the secondary index should be the updated key.
        assertEquals(1, mockServer.getSecondaryIndex().size());
        for (Key secondaryKey : mockServer.getSecondaryIndex().values()){
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

    private File getBaseFolder() {
        String os = System.getProperty("os.name").toUpperCase();
        if (os.contains("WIN")) {
            return new File(System.getenv("APPDATA") + File.separator);
        } else if (os.contains("MAC")) {
            return new File(System.getProperty("user.home") + "/Library/Application Support/");
        } else {
            return new File(System.getProperty("user.home") + File.separator + ".");
        }
    }

    private File getPersistFolder(String dataFolderName) {
        return new File(getBaseFolder(), dataFolderName);
    }

    private Persist persistSetUp() throws IOException {
        // Use a unique folder name so we do not interfere with any existing data.
        String folderName = ServerConfig.DATA_FOLDER_NAME + "-" + System.currentTimeMillis();
        persistFolder = getPersistFolder(folderName);

        // Create the folder and files
        persistFolder.mkdirs();
        File dataFile = new File(persistFolder, ServerConfig.PRIMARY_INDEX_FILE_NAME + ServerConfig.FILES_EXTENSION);
        dataFile.createNewFile();
        File indexFile = new File(persistFolder, ServerConfig.SECONDARY_INDEX_FILE_NAME + ServerConfig.FILES_EXTENSION);
        indexFile.createNewFile();

        //Create a new persist object
        return new Persist(folderName, dataFile.getName(), indexFile.getName());
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

    private VectorClock createComparableVectorClock(int serverNumber, int serverID, int offset){
        VectorClock vectorClock = new VectorClock(serverNumber, serverID);
        for(int i = 0; i < offset; i++){
            vectorClock.incrementSelfClock();
        }
        return vectorClock;
    }
}
