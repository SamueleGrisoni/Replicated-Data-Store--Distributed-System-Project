package click.replicatedDataStore.applicationLayer.serverComponents;

import click.replicatedDataStore.dataStructures.ClockedData;
import click.replicatedDataStore.dataStructures.VectorClock;
import click.replicatedDataStore.utlis.Key;
import click.replicatedDataStore.utlis.configs.ServerConfig;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

public class Persist {
    private final String dataFilePath;
    private final String indexFilePath;
    //Flag to check if the file is new. If so, the recovery methods will not access the disk to recover an empty file
    private boolean newDataFile;
    private boolean newIndexFile;

    public Persist(String dataFolderName, String dataFileName, String indexFileName) {
        String folderPath = ServerConfig.getGlobalFolderPath() + dataFolderName + File.separator;
        this.dataFilePath = folderPath + dataFileName;
        this.indexFilePath = folderPath + indexFileName;

        //create the data folder and file if it does not exist
        if (!new File(folderPath).exists()) {
            createDataFolderAndFile(folderPath, dataFilePath, indexFilePath);
        } else { //Data folder exists
            checkOrCreateDataFile(dataFilePath);
            checkOrCreateIndexFile(indexFilePath);
        }
    }

    //Append the clocked data to the data file
    public void persist(ClockedData clockedData) {
        File dataFile = new File(dataFilePath);
        if (!dataFile.exists()) {
            throw new IllegalCallerException("Data file does not exist");
        }

        ObjectOutputStream oos = createObjectOutputStream(dataFile);

        try {
            oos.writeObject(clockedData.key());
            oos.writeObject(clockedData.value());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                oos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private ObjectOutputStream createObjectOutputStream(File file){
        ObjectOutputStream oos = null;
        if(file.length() == 0){
            try{
                oos = new ObjectOutputStream(new FileOutputStream(file));
            }catch(IOException e){
                e.printStackTrace();
            }
        }else { //if the file is not empty create an oos in appended mode
            try {
                oos = new ObjectOutputStream(new FileOutputStream(file, true)) {
                    @Override
                    protected void writeStreamHeader() {
                        //Do not write the stream header when appending to the file
                    }
                };
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return oos;
    }

    //Save the clocked data to the (primary) data file, compact the primary index and overwrite the secondary index
    public void persist(ClockedData clockedData, TreeMap<VectorClock, Key> secondaryIndex){
        persist(clockedData);
        compactPrimaryIndex();

        //overwrite the index file with the new secondary index
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(indexFilePath))) {
            for (Map.Entry<VectorClock, Key> entry : secondaryIndex.entrySet()) {
                oos.writeObject(entry.getKey());
                oos.writeObject(entry.getValue());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Compact the primary index by reading the data file and writing the updated key-value pairs to a new file
    private void compactPrimaryIndex() {
        Map<Key, Serializable> newPrimaryIndex = recoverPrimaryIndex();
        if(newPrimaryIndex.isEmpty()){
            return;
        }
        //overwrite the data file with the new primary index. Because it's a map the key-value is already updated to the latest value
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(dataFilePath))) {
            for (Map.Entry<Key, Serializable> entry : newPrimaryIndex.entrySet()) {
                oos.writeObject(entry.getKey());
                oos.writeObject(entry.getValue());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public LinkedHashMap<Key, Serializable> recoverPrimaryIndex() {
        LinkedHashMap<Key, Serializable> primaryIndex = new LinkedHashMap<>();
        if (!newDataFile) {
            File dataFile = new File(dataFilePath);
            if (dataFile.length() == 0) {
                // File is empty; return an empty map without opening the stream.
                return primaryIndex;
            }
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(dataFile))) {
                while (true) {
                    Key key = (Key) ois.readObject();
                    Serializable value = (Serializable) ois.readObject();
                    primaryIndex.put(key, value);
                }
            } catch (EOFException ignored) {
                //either the file was empty or the end of the file was reached
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return primaryIndex;
    }

    public TreeMap<VectorClock, Key> recoverSecondaryIndex() {
        TreeMap<VectorClock, Key> secondaryIndex = new TreeMap<>();
        if (!newIndexFile) {
            File indexFile = new File(indexFilePath);
            if(indexFile.length() == 0){
                return secondaryIndex;
            }
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(indexFile))) {
                while (true) {
                    VectorClock vectorClock = (VectorClock) ois.readObject();
                    Key key = (Key) ois.readObject();
                    secondaryIndex.put(vectorClock, key);
                }
            } catch (EOFException ignored) {
                //Either the file was empty or the end of the file was reached. Normal behavior no action needed
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return secondaryIndex;
    }

    private void checkOrCreateDataFile(String dataFilePath) {
        File dataFile = new File(dataFilePath);
        if (!dataFile.exists()) {
            newDataFile = true;
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            newDataFile = false;
        }
    }

    private void checkOrCreateIndexFile(String indexFilePath) {
        File indexFile = new File(indexFilePath);
        if (!indexFile.exists()) {
            newIndexFile = true;
            try {
                indexFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            newIndexFile = false;
        }
    }

    private void createDataFolderAndFile(String folderPath, String dataFilePath, String indexFilePath) {
        if (!new File(folderPath).exists()) {
            newDataFile = true;
            newIndexFile = true;
            new File(folderPath).mkdir();
            try {
                new File(dataFilePath).createNewFile();
                new File(indexFilePath).createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
