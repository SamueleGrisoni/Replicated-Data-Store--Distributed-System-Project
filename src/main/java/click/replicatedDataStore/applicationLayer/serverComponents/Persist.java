package click.replicatedDataStore.applicationLayer.serverComponents;

import click.replicatedDataStore.dataStructures.ClockedData;
import click.replicatedDataStore.dataStructures.VectorClock;
import click.replicatedDataStore.utlis.Key;

import java.io.*;
import java.util.LinkedHashMap;

public class Persist {
    private final String folderPath;
    private final String dataFilePath;
    private final String indexFilePath;
    //Flag to check if the file is new. If so, the recovery methods will not access the disk to recover an empty file
    private final boolean newDataFile;
    private final boolean newIndexFile;

    public Persist(String dataFolderName, String dataFileName, String indexFileName) {
        this.folderPath = getOSDataFolderPath() + dataFolderName + File.separator;
        this.dataFilePath = folderPath + dataFileName;
        this.indexFilePath = folderPath + indexFileName;
        //create the data folder and file if it does not exist
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
        } else { //Data folder exists
            File dataFile = new File(dataFilePath);
            File indexFile = new File(indexFilePath);
            //check if data file exists
            if(!dataFile.exists()) {
                newDataFile = true;
                try {
                    dataFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                newDataFile = false;
            }
            //check if index file exists
            if(!indexFile.exists()) {
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
    }

    public void persist(ClockedData clockedData) {
        //todo
    }

    public LinkedHashMap<Key, Object> recoverPrimaryIndex() {
        LinkedHashMap<Key, Object> primaryIndex = new LinkedHashMap<>();
        if (!newDataFile) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(dataFilePath))) {
                while (true) {
                    try {
                        ClockedData clockedData = (ClockedData) ois.readObject();
                        primaryIndex.put(clockedData.key(), clockedData.value());
                    } catch (EOFException e) {
                        break; // End of file reached
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return primaryIndex;
    }

    public LinkedHashMap<VectorClock, Key> recoverSecondaryIndex() {
        LinkedHashMap<VectorClock, Key> secondaryIndex = new LinkedHashMap<>();
        if (!newDataFile) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(indexFilePath))) {
                while (true) {
                    try {
                        ClockedData clockedData = (ClockedData) ois.readObject();
                        secondaryIndex.put(clockedData.vectorClock(), clockedData.key());
                    } catch (EOFException e) {
                        break; // End of file reached
                    }
                }
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
}
