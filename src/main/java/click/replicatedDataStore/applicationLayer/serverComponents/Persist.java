package click.replicatedDataStore.applicationLayer.serverComponents;

import click.replicatedDataStore.dataStructures.ClockedData;
import click.replicatedDataStore.dataStructures.VectorClock;
import click.replicatedDataStore.utils.Key;
import click.replicatedDataStore.utils.configs.ServerConfig;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

public class Persist {
    private final String dataFilePath;
    private final String indexFilePath;
    private final String clockFilePath;
    private final String backupListFilePath;
    //Flag to check if the file is new. If so, the recovery methods will not access the disk to recover an empty file
    private boolean newDataFile;
    private boolean newIndexFile;
    private boolean newClockFile;
    private boolean newBackupListFile;

    public Persist(String dataFolderName, String dataFileName, String indexFileName, String clockFileName, String backupListFileName) {
        String folderPath = ServerConfig.getGlobalFolderPath() + dataFolderName + File.separator;
        this.dataFilePath = folderPath + dataFileName;
        this.indexFilePath = folderPath + indexFileName;
        this.clockFilePath = folderPath + clockFileName;
        this.backupListFilePath = folderPath + backupListFileName;

        //create the data folder and file if it does not exist
        if (!new File(folderPath).exists()) {
            createDataFolderAndFile(folderPath, dataFilePath, indexFilePath, clockFilePath);
        } else { //Data folder exists
            checkOrCreateDataFile(dataFilePath);
            checkOrCreateIndexFile(indexFilePath);
            checkOrCreateClockFile(clockFilePath);
            checkOrCreateBackupListFile(backupListFilePath);
        }
    }

    //Append the clocked data to the data file
    public void persist(ClockedData clockedData) {
        File dataFile = new File(dataFilePath);
        if (!dataFile.exists()) {
            throw new IllegalCallerException("Data file does not exist");
        }
        ObjectOutputStream oos = createObjectOutputStream(dataFile, true);
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

    private ObjectOutputStream createObjectOutputStream(File file, boolean append) {
        if (!append || file.length() == 0) {
            try {
                return new ObjectOutputStream(new FileOutputStream(file));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                return new ObjectOutputStream(new FileOutputStream(file, true)) {
                    @Override
                    protected void writeStreamHeader() {
                        // Do not write the stream header when appending to the file
                    }
                };
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.err.println("ObjectOutputStream is null");
        return null;
    }

    public void persist(TreeMap<VectorClock, Integer> secondaryIndex) {
        compactPrimaryIndex();
        //overwrite the index file with the new secondary index
        File indexFile = new File(indexFilePath);
        if (!indexFile.exists()) {
            throw new IllegalCallerException("Index file does not exist");
        }
        ObjectOutputStream oos = createObjectOutputStream(indexFile, false);
        try {
            for (Map.Entry<VectorClock, Integer> entry : secondaryIndex.entrySet()) {
                oos.writeObject(entry.getKey());
                oos.writeObject(entry.getValue());
            }
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

    //Save the vector clock to the clock file
    public void persist(VectorClock vectorClock) {
        //System.out.println("Persisting vector clock: " + vectorClock);
        File clockFile = new File(clockFilePath);
        if (!clockFile.exists()) {
            throw new IllegalCallerException("Clock file does not exist");
        }
        ObjectOutputStream oos = createObjectOutputStream(clockFile, false);
        try {
            oos.writeObject(vectorClock);
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

    //Overwrite the backup list file with the new backup list
    public void persist(BackupList backupList){
        File backupListFile = new File(backupListFilePath);
        if (!backupListFile.exists()) {
            throw new IllegalCallerException("Backup list file does not exist");
        }
        ObjectOutputStream oos = createObjectOutputStream(backupListFile, false);
        try {
            oos.writeObject(backupList);
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

    //Compact the primary index by reading the data file and writing the updated key-value pairs to a new file
    private void compactPrimaryIndex() {
        Map<Key, Serializable> newPrimaryIndex = recoverPrimaryIndex();
        if (newPrimaryIndex.isEmpty()) {
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

    public VectorClock recoverVectorClock(String serverName, int serverNumber, int serverIndex) {
        VectorClock vc;
        if (!newClockFile) {
            File clockFile = new File(clockFilePath);
            if (clockFile.length() == 0) {
                vc = new VectorClock(serverName, serverNumber, serverIndex);
                System.out.println("Clock file is empty. Creating a new vector clock " + vc);
                return vc;
            }
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(clockFile))) {
                vc = (VectorClock) ois.readObject();
                System.out.println("Recovering vector clock: " + vc);
                return vc;
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        vc = new VectorClock(serverName, serverNumber, serverIndex);
        System.out.println("Clock file is new. Creating a new vector clock " + vc);
        return vc;
    }

    public BackupList recoverBackupList() {
        BackupList backupList = new BackupList();
        if (!newBackupListFile) {
            File backupListFile = new File(backupListFilePath);
            if (backupListFile.length() == 0) {
                // File is empty; return an empty BackupList without opening the stream.
                return backupList;
            }
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(backupListFile))) {
                backupList = (BackupList) ois.readObject();
                System.out.println("Recovering backup list: " + backupList);
                return backupList;
            } catch (EOFException ignored) {
                // Either the file was empty or the end of the file was reached. Normal behavior, no action needed.
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Backup list is new. Returning an empty BackupList.");
        return backupList;
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

    public TreeMap<VectorClock, Integer> recoverSecondaryIndex() {
        TreeMap<VectorClock, Integer> secondaryIndex = new TreeMap<>();
        if (!newIndexFile) {
            File indexFile = new File(indexFilePath);
            if (indexFile.length() == 0) {
                return secondaryIndex;
            }
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(indexFile))) {
                while (true) {
                    VectorClock vectorClock = (VectorClock) ois.readObject();
                    Integer index = (Integer) ois.readObject();
                    secondaryIndex.put(vectorClock, index);
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

    private void checkOrCreateClockFile(String clockFilePath) {
        File clockFile = new File(clockFilePath);
        if (!clockFile.exists()) {
            newClockFile = true;
            try {
                clockFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            newClockFile = false;
        }
    }

    private void createDataFolderAndFile(String folderPath, String dataFilePath, String indexFilePath, String clockFilePath) {
        if (!new File(folderPath).exists()) {
            newDataFile = true;
            newIndexFile = true;
            newClockFile = true;
            newBackupListFile = true;
            new File(folderPath).mkdir();
            try {
                new File(dataFilePath).createNewFile();
                new File(indexFilePath).createNewFile();
                new File(clockFilePath).createNewFile();
                new File(backupListFilePath).createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void checkOrCreateBackupListFile(String backupListFilePath) {
        File backupListFile = new File(backupListFilePath);
        if (!backupListFile.exists()) {
            newBackupListFile = true;
            try {
                backupListFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            newBackupListFile = false;
        }
    }
}
