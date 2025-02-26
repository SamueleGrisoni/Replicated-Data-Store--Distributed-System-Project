package click.replicatedDataStore.applicationLayer.serverComponents;

import click.replicatedDataStore.applicationLayer.Server;
import click.replicatedDataStore.applicationLayer.serverComponents.dataManager.DataManagerReader;
import click.replicatedDataStore.connectionLayer.connectionManagers.ServerConnectionManager;
import click.replicatedDataStore.connectionLayer.messages.FetchMsg;
import click.replicatedDataStore.connectionLayer.messages.HeavyPushMsg;
import click.replicatedDataStore.connectionLayer.messages.LightPushMsg;
import click.replicatedDataStore.dataStructures.ClockedData;
import click.replicatedDataStore.dataStructures.VectorClock;
import click.replicatedDataStore.utlis.ClockTooFarAhead;
import click.replicatedDataStore.utlis.Config;
import click.replicatedDataStore.utlis.Key;

import java.util.*;

public class TimeTravel {
    private final Server server;
    private ServerConnectionManager serverConnectionManager;
    //todo why we need this here?
    private final DataManagerReader dataManagerReader;
    private final Thread lightPusher;
    private boolean stopLightPusher = false;

    public TimeTravel(Server server, DataManagerReader dataManagerReader) {
        this.server = server;
        this.dataManagerReader = dataManagerReader;
        this.lightPusher = new Thread(this::lightPusherFunction);
        this.serverConnectionManager = new ServerConnectionManager(server.getMyAddressAndPortPair().first(), server.getMyAddressAndPortPair().second());
    }

    private void lightPusherFunction() {
        while (!stopLightPusher) {
            LightPushMsg lightPushMsg = new LightPushMsg(server.getVectorClock());
            //todo implement a method to send msg to all servers
            //serverConnectionManager.sendToAll(lightPushMsg);
            System.out.println("LightPusher: " + lightPushMsg);
            try {
                Thread.sleep(Config.LIGHT_PUSH_DELAY_MILLIS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    //Function call each time I receive a LightPushMsg
    public void checkMySync(VectorClock otherVectorClock) {
        VectorClock serverVectorClock = server.getVectorClock();
        try{
            //todo fix case in which the other clock is ahead of 1.
            VectorClock.checkIfUpdatable(serverVectorClock, otherVectorClock);
        }catch (ClockTooFarAhead e){
            computeFetch(serverVectorClock, otherVectorClock);
        }catch (IllegalArgumentException e){
            System.out.println("The incoming vector clock is invalid");
            e.printStackTrace();
        }
    }

    private void computeFetch(VectorClock serverVectorClock, VectorClock otherVectorClock) {
        //todo i'm not so sure what the communication layer need in order to respond to the sender
        FetchMsg fetchMsg = new FetchMsg(serverVectorClock);
        System.out.println("Fetch: " + fetchMsg);
        serverConnectionManager.sendTo(server.getMyAddressAndPortPair().first(), fetchMsg);
    }

    //Function called when the sending server realized that he is not up to date, and ask for a fetch
    public void helpOtherSync(VectorClock otherVectorClock) {
        VectorClock serverVectorClock = server.getVectorClock();
        //todo how can HeavyPushMsg be a list of ClockedData if I dont have the vector clock of each entry?
        LinkedHashMap<Key, Object> primaryIndex = server.getPrimaryIndex();

        //create a sorted map based on the vector clock
        TreeMap<VectorClock, Key> secondaryIndex = new TreeMap<>(server.getSecondaryIndex());
        //find the max vc (key) that is smaller than the otherVectorClock
        Key maxSmallerKey = secondaryIndex.get(secondaryIndex.floorKey(otherVectorClock));

        //create a list of ClockedData that will be pushed
        List<ClockedData> missingData = computeMissingData(primaryIndex, serverVectorClock, maxSmallerKey);
        HeavyPushMsg heavyPushMsg = new HeavyPushMsg(missingData);
        //todo implement a method to send msg to the server that requested the fetch
        //serverConnectionManager.sendToServer(heavyPushMsg, otherAddress);
    }

    private List<ClockedData> computeMissingData(Map<Key, Object> primaryIndex, VectorClock serverVectorClock, Key maxSmallerKey) {
        List<ClockedData> missingData = new ArrayList<>();
        boolean found = false;
        for(Map.Entry<Key, Object> entry : primaryIndex.entrySet()){
            if(found){
                missingData.add(new ClockedData(serverVectorClock, entry.getKey(), entry.getValue()));
            }
            if(entry.getKey().equals(maxSmallerKey)){
                found = true;
                missingData.add(new ClockedData(serverVectorClock, entry.getKey(), entry.getValue()));
            }
        }
        return missingData;
    }

    public void startLightPusher() {
        stopLightPusher = false;
        lightPusher.start();
    }

    public void stopLightPusher() {
        stopLightPusher = true;
    }

    //atm used only for testing
    public ServerConnectionManager getServerConnectionManager() {
        return serverConnectionManager;
    }
    //atm used only for testing
    public void setServerConnectionManager(ServerConnectionManager serverConnectionManager) {
        this.serverConnectionManager = serverConnectionManager;
    }
}
