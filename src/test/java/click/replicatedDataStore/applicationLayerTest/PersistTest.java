package click.replicatedDataStore.applicationLayerTest;

import click.replicatedDataStore.applicationLayer.serverComponents.Persist;
import click.replicatedDataStore.dataStructures.ClockedData;
import click.replicatedDataStore.dataStructures.VectorClock;
import click.replicatedDataStore.utils.configs.ServerConfig;
import click.replicatedDataStore.utils.Key;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PersistTest implements Serializable {
    private Persist persist;
    private String folderName;
    private File persistFolder;
    private File dataFile;
    private File indexFile;

    private File getPersistFolder(String dataFolderName) {
        return new File(ServerConfig.getGlobalFolderPath(), dataFolderName);
    }

    //Before each test create a temporary dataFolder and files
    @Before
    public void setUp() throws Exception {
        // Use a unique folder name so we do not interfere with any existing data.
        folderName = ServerConfig.SERVER_DATA_FOLDER_NAME + "-" + System.currentTimeMillis();
        persistFolder = getPersistFolder(folderName);

        // Create the folder and files
        persistFolder.mkdirs();
        dataFile = new File(persistFolder, ServerConfig.PRIMARY_INDEX_FILE_NAME + ServerConfig.FILES_EXTENSION);
        dataFile.createNewFile();
        indexFile = new File(persistFolder, ServerConfig.SECONDARY_INDEX_FILE_NAME + ServerConfig.FILES_EXTENSION);
        indexFile.createNewFile();

        //Create a new persist object
        persist = new Persist(folderName, dataFile.getName(), indexFile.getName());
    }

    //After each recursively remove the temporary folder and files
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

    //Test if temp folder and files are created correctly
    @Test
    public void testFolderAndFileCreation() {
        assertTrue(persistFolder.exists());
        assertTrue(dataFile.exists());
        assertTrue(indexFile.exists());
    }

    //File exists, but it is empty, it should still return an empty map
    @Test
    public void testRecoverPrimaryIndexEmpty() {
        LinkedHashMap<Key, Serializable> recovered = persist.recoverPrimaryIndex();
        assertTrue(recovered.isEmpty());
    }

    @Test
    public void testRecoverSecondaryIndexEmpty() {
        TreeMap<VectorClock, Key> recovered = persist.recoverSecondaryIndex();
        assertTrue(recovered.isEmpty());
    }

    // Test persisting and recovering a primary index.
    @Test
    public void testPrimaryIndexPersist() {
        //Create clockedData
        VectorClock vectorClock = new VectorClock("server0", 3, 0);
        Key key = new TestKey("key");
        Key key2 = new TestKey("key2");
        ClockedData clockedData = new ClockedData(vectorClock, key , "value1");

        //Persist the clockedData (basically updating the primary index file)
        persist.persist(clockedData);
        vectorClock.incrementSelfClock();
        ClockedData clockedData2 = new ClockedData(vectorClock, key2, "value2");
        persist.persist(clockedData2);

        LinkedHashMap<Key, Serializable> recovered = persist.recoverPrimaryIndex();
        for (Map.Entry<Key, Serializable> entry : recovered.entrySet()) {
            System.out.println("Key: " + entry.getKey() + " Object: " + entry.getValue());
        }
        assertTrue(recovered.containsKey(key));
        assertEquals("value1", recovered.get(key));
        assertTrue(recovered.containsKey(key2));
        assertEquals("value2", recovered.get(key2));
    }

    //Test persisting of primary and secondary index
    @Test
    public void testPrimarySecondaryIndex() {
        LinkedHashMap<Key, Object> primaryIndex = new LinkedHashMap<>();
        TreeMap<VectorClock, Key> secondaryIndex = new TreeMap<>();

        //Create clockedData
        VectorClock vectorClock = new VectorClock("server0", 3, 0);
        vectorClock.incrementSelfClock(); //vc = [1, 0, 0]
        TestKey key = new TestKey("key1");
        ClockedData clockedData = new ClockedData(vectorClock, key, "value1");
        //populate the primary Index
        primaryIndex.put(key, "value1");
        //populate the secondary Index
        secondaryIndex.put(vectorClock, key);

        persist.persist(clockedData, secondaryIndex);

        LinkedHashMap<Key, Serializable> recoveredPrimary = persist.recoverPrimaryIndex();
        for (Map.Entry<Key, Serializable> entry : recoveredPrimary.entrySet()) {
            System.out.println("Key: " + entry.getKey() + " Object: " + entry.getValue());
        }
        assertTrue(recoveredPrimary.containsKey(key));
        assertEquals("value1", recoveredPrimary.get(key));

        TreeMap<VectorClock, Key> recoveredSecondary = persist.recoverSecondaryIndex();
        for (Map.Entry<VectorClock, Key> entry : recoveredSecondary.entrySet()) {
            System.out.println("VectorClock: " + entry.getKey() + " Key: " + entry.getValue());
        }
        assertTrue(recoveredSecondary.containsKey(vectorClock));
        assertEquals(recoveredSecondary.get(vectorClock), key);
    }

    @Test
    public void testUpdatePrimaryIndex(){
        VectorClock vectorClock = new VectorClock("server0", 3, 0);
        Key key = new TestKey("key");
        ClockedData clockedData = new ClockedData(vectorClock, key , "value1");
        persist.persist(clockedData);
        vectorClock.incrementSelfClock();
        ClockedData clockedData2 = new ClockedData(vectorClock, key, "value2");
        persist.persist(clockedData2);
        LinkedHashMap<Key, Serializable> recovered = persist.recoverPrimaryIndex();
        assertEquals(1, recovered.size());
        assertEquals("value2", recovered.get(key));
    }

    //Dummy key used for testing
    private record TestKey(String keyValue) implements Key {

        @Override
        public String toString() {
            return keyValue;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            TestKey testKey = (TestKey) o;
            return testKey.keyValue().equals(keyValue);
        }

        @Override
        public int hashCode() {
            return keyValue.hashCode();
        }
    }
}
