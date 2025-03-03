package click.replicatedDataStore;

import click.replicatedDataStore.applicationLayer.clientComponents.RequestSender;
import click.replicatedDataStore.applicationLayer.clientComponents.view.ClientErrorManager;
import click.replicatedDataStore.dataStructures.keyImplementations.StringKey;

import java.util.Scanner;

public class Client {

    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);
        ClientErrorManager eMan = new ClientErrorManager();

        System.out.println("Just a simple demo: string -> int");

        System.out.println("ip: ");
        String ip = input.nextLine();
        System.out.println("port: ");
        int port = input.nextInt();

        RequestSender sender = new RequestSender(ip, port, eMan);

        System.out.println("what do you want to do?\n");
        while(true){
            System.out.println("0: read\n" + "1: write\n");

            int choice = input.nextInt();
            if(choice == 0){
                System.out.println("key: ");
                String skey = input.nextLine();

                sender.read(new StringKey(skey));
            } else if (choice == 1) {
                System.out.println("key: ");
                String skey = input.nextLine();
                System.out.println("value: ");
                Integer val = input.nextInt();

                sender.write(new StringKey(skey), val);
            }else {
                System.out.println("Wrong input\n");
            }
        }
    }
}
