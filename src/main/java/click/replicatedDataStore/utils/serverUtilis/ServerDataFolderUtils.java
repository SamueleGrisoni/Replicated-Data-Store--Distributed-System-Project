package click.replicatedDataStore.utils.serverUtilis;

import click.replicatedDataStore.dataStructures.Pair;
import click.replicatedDataStore.utils.configs.ConfigFile;
import click.replicatedDataStore.utils.configs.ServerConfig;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ServerDataFolderUtils {

    protected static void checkConfigFileHasChanged(Pair<List<ConfigFile.ConfigFileEntry>, List<ConfigFile.ConfigFileEntry>> addressesListPair) {
        List<ConfigFile.ConfigFileEntry> totalList = copyAndSanitizeList(addressesListPair);
        System.out.println("TotalList:" + totalList);

        String folderPath = getOSFolderPath() + ServerConfig.GLOBAL_FOLDER_NAME + File.separator;
        String configFilePath = folderPath + ServerConfig.CONFIG_FILE_HASH_NAME + ServerConfig.FILES_EXTENSION;
        if (!new File(folderPath).exists()) {
            System.out.println("No old data found, creating new data folder");
            createGlobalDataFolder(new File(folderPath));
            writeList(configFilePath, totalList);
        } else {
            try (InputStream in = new FileInputStream(configFilePath)) {
                //todo check why the new hash is always different from the old one
                String oldHash = new String(in.readAllBytes());
                in.close();
                String newHash = computeHash(totalList);
                if (!oldHash.equals(newHash)) {
                    System.out.println("Config file has changed, deleting old data folder and creating a new one");
                    recreateGlobalDataFolder(folderPath, configFilePath, totalList);
                }
            }catch (FileNotFoundException | EOFException e){
                System.out.println("Config file is corrupted, deleting old data folder and creating a new one");
                recreateGlobalDataFolder(folderPath, configFilePath, totalList);
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Failed to read the config file hash");
                System.exit(1);
            }
        }
    }

    private static void deleteDataFolder(File dataFolder) {
        System.out.println("Deleting data folder: " + dataFolder);
        File[] files = dataFolder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDataFolder(file);
                } else {
                    if (!file.delete()) {
                        deleteDataFolder(file);
                    }
                }
            }
        }
        if (!dataFolder.delete()) {
            System.out.println("Failed to delete the data folder: " + dataFolder);
        }
    }

    private static void createGlobalDataFolder(File folderPath) {
        try {
            if(!folderPath.mkdirs()){
                System.out.println("Failed to create the data folder");
                System.exit(1);
            }
        } catch (SecurityException e) {
            System.out.println("Failed to create the data folder");
            System.exit(1);
        }
    }

    private static void recreateGlobalDataFolder(String folderPath, String configFilePath, List<ConfigFile.ConfigFileEntry> totalList) {
        deleteDataFolder(new File(folderPath));
        createGlobalDataFolder(new File(folderPath));
        writeList(configFilePath, totalList);
    }

    private static void writeList(String configFilePath, List<ConfigFile.ConfigFileEntry> totalList) {
        try {
            String hash = computeHash(totalList);
            try (FileOutputStream out = new FileOutputStream(configFilePath)) {
                out.write(hash.getBytes());
            }
        }catch (IOException e){
            e.printStackTrace();
            System.out.println("Failed to write the config file hash");
            System.exit(1);
        }
    }

    private static String computeHash(List<ConfigFile.ConfigFileEntry> totalList) {
        try{
            //Serialize the list
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(totalList);
            objectOutputStream.flush();
            byte[] serializedData = byteArrayOutputStream.toByteArray();

            // Compute the SHA-256 hash so it's stable between runs
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(serializedData);

            // Convert the hash bytes to a hexadecimal string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        }catch (IOException | NoSuchAlgorithmException e){
            e.printStackTrace();
            System.out.println("Failed to compute the hash of the config file");
            System.exit(1);
            return null;
        }
    }

    private static String getOSFolderPath() {
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

    private static List<ConfigFile.ConfigFileEntry> copyAndSanitizeList(Pair<List<ConfigFile.ConfigFileEntry>, List<ConfigFile.ConfigFileEntry>> configListPair) {
        List<ConfigFile.ConfigFileEntry> totalList = new ArrayList<>();

        for (ConfigFile.ConfigFileEntry entry : configListPair.first()) {
            ConfigFile.ConfigFileEntry newEntry = new ConfigFile.ConfigFileEntry(entry);
            newEntry.setIsPersistent(null);
            newEntry.setHeavyPropagationPolicy(null);
            newEntry.setOverlayNetHeavy(null);
            newEntry.setOverlayNetLight(null);
            totalList.add(newEntry);
        }
        for (ConfigFile.ConfigFileEntry entry : configListPair.second()) {
            ConfigFile.ConfigFileEntry newEntry = new ConfigFile.ConfigFileEntry(entry);
            newEntry.setIsPersistent(null);
            newEntry.setHeavyPropagationPolicy(null);
            newEntry.setOverlayNetHeavy(null);
            newEntry.setOverlayNetLight(null);
            totalList.add(newEntry);
        }
        totalList.sort(Comparator.comparing(ConfigFile.ConfigFileEntry::getServerName));
        return totalList;
    }
}
