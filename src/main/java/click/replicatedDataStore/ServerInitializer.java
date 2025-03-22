package click.replicatedDataStore;

import click.replicatedDataStore.dataStructures.Pair;
import click.replicatedDataStore.dataStructures.ServerPorts;
import click.replicatedDataStore.utlis.serverUtilis.ServerInitializerUtils;

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
}
