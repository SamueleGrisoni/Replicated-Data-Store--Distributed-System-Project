package click.replicatedDataStore;

import click.replicatedDataStore.applicationLayer.clientComponents.RequestSender;
import click.replicatedDataStore.applicationLayer.clientComponents.view.ClientErrorManager;
import click.replicatedDataStore.connectionLayer.messages.AnswerState;
import click.replicatedDataStore.dataStructures.ClientWrite;
import click.replicatedDataStore.dataStructures.keyImplementations.StringKey;

import java.util.InputMismatchException;
import java.util.Scanner;

public class Client {

    public static int nextIntClear(){
        Scanner input = new Scanner(System.in);
        int val = 0;
        boolean valid = false;
        while (!valid) {
            try {
                val = input.nextInt();
                input.nextLine();
                valid = true;
            } catch (InputMismatchException e) {
                System.out.println("Invalid input. Please, for this demo, enter an integer as value.");
                input.nextLine();
            }
        }
        return val;
    }

    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);
        ClientErrorManager eMan = new ClientErrorManager();

        System.out.println("Just a simple demo: string -> int");

        System.out.println("ip: ");
        String ip = input.nextLine();
        System.out.println("port: ");
        int port = nextIntClear();

        RequestSender sender = new RequestSender(ip.isEmpty()? "localhost": ip, port, eMan);

        System.out.println("what do you want to do?");
        while(true){
            System.out.println("0: read"+ " --- " + "1: write");

            int choice = nextIntClear();
            if(choice == 0){
                System.out.println("key: ");
                String skey = input.nextLine();
                System.out.println("reading (" + skey + ") ...");
                ClientWrite read = sender.read(new StringKey(skey));
                System.out.println(read.key() + ", " + read.value());
            } else if (choice == 1) {
                System.out.println("key: ");
                String skey = input.nextLine();
                System.out.println("value: ");
                Integer val = nextIntClear();
                System.out.println("writing (" + skey + ", " + val + ") ...");
                AnswerState state = sender.write(new StringKey(skey), val);
                System.out.println(state);
            }else {
                System.out.println("Wrong input\n");
            }
        }
    }
}
