package click.replicatedDataStore;

import click.replicatedDataStore.dataStructures.Pair;
import click.replicatedDataStore.dataStructures.ServerPorts;
import click.replicatedDataStore.utlis.ServerInitializerUtils;
import java.util.*;

public class ServerInitializer {
    public static void main(String[] args) {
        if(args.length != 1){
            System.out.println("Usage: java -jar Server.jar configFilePath");
            System.exit(1);
        }

        ServerInitializerUtils SIU = new ServerInitializerUtils();

        Map<Integer, Pair<String, ServerPorts>> addresses = SIU.computeAddress(args[0]);
        System.out.println("Found a config file for " + addresses.size() + " servers");
        SIU.printServerList();

        SIU.startLocalServer();

        Scanner scanner = new Scanner(System.in);
        while(true){
            System.out.println("To stop or restart a local server enter the server id. Type 'exit' to stop all the local servers: ");
            String input = scanner.nextLine();
            if(input.equals("exit")){
                SIU.closeAllLocalServer();
                break;
            }
            try{
                int serverId = Integer.parseInt(input);
                SIU.stopOrStartLocalServer(serverId);
            }catch (NumberFormatException e){
                System.out.println("Invalid input. Please enter a local server index or 'exit'");
            }
        }
        System.out.println("Local servers stopped successfully");
        System.exit(0);
    }
}
