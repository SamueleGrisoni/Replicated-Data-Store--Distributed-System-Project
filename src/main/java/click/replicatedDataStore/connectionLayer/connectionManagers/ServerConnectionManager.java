package click.replicatedDataStore.connectionLayer.connectionManagers;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;

public class ServerConnectionManager extends ConnectionManager{
    private HashMap<String, Runnable> routingTable;
    private ObjectOutputStream[] outChannels;
    private ObjectInputStream[] inChannels;
    private final Object que; //TODO make it the server queue
    private final Object sync; //TODO make it the server timeTravel component

    //TODO get as parameter the hashmap with the routes
    ServerConnectionManager(String ip, Integer port) {
        //TODO create connection acceptor thread
        this.routingTable = new HashMap<>(); //TODO add routing
        try {
            Socket s = new Socket(ip, port);
            //TODO add input and output streams
        }catch (IOException e) {
            e.getMessage(); //TODO handle exception
        }

        que = new Object(); //TODO make it the server queue
        sync = new Object(); //TODO make it the server timeTravel component
    }

    public HashMap<String, Runnable> getRoutes() {
        return routingTable;
    }

    public void addInChannels(ObjectInputStream in, int index){
        this.inChannels[index] = in;
    }

    public void addOutChannels(ObjectOutputStream out, int index){
        this.outChannels[index] = out;
    }
}
