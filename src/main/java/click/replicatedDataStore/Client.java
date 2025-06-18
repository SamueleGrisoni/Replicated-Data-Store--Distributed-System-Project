package click.replicatedDataStore;

import click.replicatedDataStore.applicationLayer.clientComponents.RequestSender;
import click.replicatedDataStore.applicationLayer.clientComponents.view.ClientErrorManager;
import click.replicatedDataStore.connectionLayer.messages.AnswerState;
import click.replicatedDataStore.dataStructures.ClientWrite;
import click.replicatedDataStore.dataStructures.Pair;
import click.replicatedDataStore.dataStructures.keyImplementations.StringKey;

import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

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
        boolean stop = false;

        System.out.println("Just a simple demo: string -> int");
        while (!stop) {
            System.out.println("ip: ");
            String ip = input.nextLine();
            System.out.println("port: ");
            int port = nextIntClear();

            RequestSender sender = new RequestSender(ip.isEmpty() ? "localhost" : ip, port, eMan);

            System.out.println("what do you want to do?");
            AtomicBoolean restart = new AtomicBoolean(false);

            Map<Integer, Pair<String, Supplier<Void>>> choiceMap = getChoiceMap(restart, input, sender);

            while (!restart.get()) {
                System.out.println("-------------------------");
                for (Map.Entry<Integer, Pair<String, Supplier<Void>>> entry : choiceMap.entrySet()) {
                    System.out.println(entry.getKey() + ": " + entry.getValue().first());
                }
                int choice = nextIntClear();
                if (choiceMap.containsKey(choice)) {
                    choiceMap.get(choice).second().get();
                } else {
                    System.out.println("Wrong input\n");
                }
            }
            //System.out.println("continue? (0 = n / 1 = y): ");
            //if (nextIntClear() == 0) stop = true;
        }
    }

    private static Map<Integer, Pair<String, Supplier<Void>>> getChoiceMap(AtomicBoolean stop, Scanner input, RequestSender sender) {
        Map<Integer, Pair<String, Supplier<Void>>> choiceMap = new HashMap<>();
        choiceMap.put(0, new Pair<>("exit", () -> {
            System.out.println("exiting...");
            stop.set(true);
            return null;
        }));
        choiceMap.put(1, new Pair<>("read", () -> {
            System.out.println("key: ");
            String skey = input.nextLine();
            System.out.println("reading (" + skey + ") ...");
            ClientWrite read = sender.read(new StringKey(skey));
            System.out.println(read.key() + ", " + read.value());
            return null;
        }));
        choiceMap.put(2, new Pair<>("write", () -> {
            System.out.println("key: ");
            String skey = input.nextLine();
            System.out.println("value: ");
            Integer val = nextIntClear();
            System.out.println("writing (" + skey + ", " + val + ") ...");
            AnswerState state = sender.write(new StringKey(skey), val);
            System.out.println(state);
            return null;
        }));
        return choiceMap;
    }
}
