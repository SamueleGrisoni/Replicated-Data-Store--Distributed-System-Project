package click.replicatedDataStore;

import click.replicatedDataStore.dataStructures.Pair;
import click.replicatedDataStore.dataStructures.ServerPorts;
import click.replicatedDataStore.utlis.configs.ConfigFile;
import click.replicatedDataStore.utlis.configs.ServerConfig;
import click.replicatedDataStore.utlis.serverUtilis.ServerInitializerUtils;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;

public class ServerInitializer {
    public static void main(String[] args) {
        if(ServerConfig.debug)
            showWorkingDirectory();

        if(args.length != 1){
            System.out.println("Usage: java -jar Server.jar configFilePath");
            System.exit(1);
        }

        ServerInitializerUtils SIU = new ServerInitializerUtils();

        Map<Integer, Pair<String, ServerPorts>> addresses = SIU.computeAddress(args[0]);
        System.out.println("Found a config file for " + addresses.size() + " servers");
        SIU.printServerList();

        SIU.startAllLocalServer();

        Scanner scanner = new Scanner(System.in);
        while(true){
            System.out.println("To stop or restart a local server enter the server name. Type 'exit' to stop all the local servers: ");
            String input = scanner.nextLine();
            if(input.equals("exit")){
                SIU.closeAllLocalServer();
                break;
            }
            SIU.stopOrStartLocalServer(input);
        }
        System.exit(0);
    }

    private static void showWorkingDirectory(){
        String currentDirPath = System.getProperty("user.dir");
        File currentDir = new File(currentDirPath);
        File[] containedFiles = currentDir.listFiles();

        System.out.println("Working in directory:  " + currentDirPath + "\n" +
                           "Contents:");
        assert containedFiles != null;
        for(File file : containedFiles){
            System.out.println(file.getName());
        }
    }
}
