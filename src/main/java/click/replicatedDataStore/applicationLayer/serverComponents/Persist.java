package click.replicatedDataStore.applicationLayer.serverComponents;

import click.replicatedDataStore.dataStructures.VectorClock;
import click.replicatedDataStore.utlis.Key;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class Persist {
    private final String folderPath;
    private final String dataFilePath;
    private final String indexFilePath;
    //Flag to check if the file is new. If so, the recovery methods will not access the disk to recover an empty file
    private boolean newDataFile;
    private boolean newIndexFile;

    public Persist(String dataFolderName, String dataFileName, String indexFileName) {
        this.folderPath = getOSDataFolderPath() + dataFolderName + File.separator;
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

    //Persist the primary index to the data file
    public void persist(Map<Key, Object> primaryIndex) {
        File dataFile = new File(dataFilePath);
        if (!dataFile.exists()) {
            throw new IllegalCallerException("Data file does not exist");
        }
        dataFile.delete();
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(dataFilePath))) {
            for (Map.Entry<Key, Object> entry : primaryIndex.entrySet()) {
                oos.writeObject(entry.getKey());
                oos.writeObject(entry.getValue());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Persist the secondary index to the index file
    public void persist(Map<Key, Object> primaryIndex, Map<VectorClock, Key> secondaryIndex) {
        File dataFile = new File(dataFilePath);
        File indexFile = new File(indexFilePath);
        if (!dataFile.exists() || !indexFile.exists()) {
            throw new IllegalCallerException("Data file or secondary index file do not exist");
        }
        dataFile.delete();
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(dataFilePath))) {
            for (Map.Entry<Key, Object> entry : primaryIndex.entrySet()) {
                oos.writeObject(entry.getKey());
                oos.writeObject(entry.getValue());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        indexFile.delete();
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(indexFilePath))) {
            for (Map.Entry<VectorClock, Key> entry : secondaryIndex.entrySet()) {
                oos.writeObject(entry.getKey());
                oos.writeObject(entry.getValue());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public LinkedHashMap<Key, Object> recoverPrimaryIndex() {
        LinkedHashMap<Key, Object> primaryIndex = new LinkedHashMap<>();
        if (!newDataFile) {
            File dataFile = new File(dataFilePath);
            if (dataFile.length() == 0) {
                // File is empty; return an empty map without opening the stream.
                return primaryIndex;
            }
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(dataFile))) {
                while (true) {
                    Key key = (Key) ois.readObject();
                    Object value = ois.readObject();
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

    public LinkedHashMap<VectorClock, Key> recoverSecondaryIndex() {
        LinkedHashMap<VectorClock, Key> secondaryIndex = new LinkedHashMap<>();
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

    /**
     * Get the path of the data folder based on the OS (Windows, macOS, Linux)
     * data folder is the folder where the data is going to be persisted
     *
     * @return the path of the data folder depending on the OS
     */
    private String getOSDataFolderPath() {
        String OS = (System.getProperty("os.name")).toUpperCase();
        if (OS.contains("WIN")) {
            return System.getenv("APPDATA") + File.separator;
        } else if (OS.contains("MAC")) {
            //well-known hardcoded paths, used in the appDirs library
            return System.getProperty("user.home") + "/Library/Application Support/";
        } else {
            //In a Unix system, data will be saved in the home directory as a hidden folder
            return System.getProperty("user.home") + File.separator + ".";
        }
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
