package click.replicatedDataStore;

import click.replicatedDataStore.dataStructures.Pair;
import click.replicatedDataStore.dataStructures.ServerPorts;
import click.replicatedDataStore.utils.configs.ServerConfig;
import click.replicatedDataStore.utils.serverUtilis.ServerInitializerUtils;

import java.io.File;
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
            System.out.println("To stop, restart or disconnect a local server enter the server name. Type 'exit' to stop all the local servers: ");
            String userInput = scanner.nextLine();
            if(userInput.equals("exit")){
                SIU.closeAllLocalServer();
                break;
            }
            if (!SIU.isLocalServer(userInput)){
                System.out.println("Server " + userInput + " is not a name of a local server");
            }else{
                if (SIU.isServerRunning(userInput)) {
                    System.out.println("Server " + userInput + " is running. Do you want to disconnect (d) or stop (s) it?");
                    String actionSelect = scanner.nextLine();
                    while (!actionSelect.equals("d") && !actionSelect.equals("s")) {
                        System.out.println("Please enter 'd' to disconnect or 's' to stop the server.");
                        actionSelect = scanner.nextLine();
                    }
                    if (actionSelect.equals("d")) {
                        //Todo add here server disconnect
                        System.out.println("TODO disconnect server " + userInput);
                    }else {
                        System.out.println("Stopping server " + userInput);
                        SIU.stopLocalServer(userInput);
                    }
                }else{
                    SIU.startLocalServer(userInput);
                }
            }
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
