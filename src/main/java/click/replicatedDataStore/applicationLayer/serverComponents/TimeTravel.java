package click.replicatedDataStore.applicationLayer.serverComponents;

import click.replicatedDataStore.applicationLayer.Server;
import click.replicatedDataStore.applicationLayer.serverComponents.dataManager.DataManagerReader;
import click.replicatedDataStore.applicationLayer.serverComponents.dataManager.VectorClockComparation;
import click.replicatedDataStore.connectionLayer.connectionManagers.ServerConnectionManager;
import click.replicatedDataStore.connectionLayer.messages.AbstractMsg;
import click.replicatedDataStore.connectionLayer.messages.ServerFetchMsg;
import click.replicatedDataStore.connectionLayer.messages.ServerHeavyPushMsg;
import click.replicatedDataStore.connectionLayer.messages.ServerLightPushMsg;
import click.replicatedDataStore.dataStructures.ClockedData;
import click.replicatedDataStore.dataStructures.VectorClock;
import click.replicatedDataStore.utlis.Key;
import click.replicatedDataStore.utlis.ServerConfig;

import java.util.*;

public class TimeTravel {
    private final Server server;
    private final ServerConnectionManager serverConnectionManager;
    private final DataManagerReader dataManagerReader;
    private final ClientServerPriorityQueue que;
    private final Thread lightPusher;
    private boolean stopLightPusher = false;
    private final Random rand = new Random();

    public TimeTravel(Server server, DataManagerReader dataManagerReader,
                      ServerConnectionManager connectionManager, ClientServerPriorityQueue que) {
        this.server = server;
        this.dataManagerReader = dataManagerReader;
        this.lightPusher = new Thread(this::lightPusherFunction);
        this.serverConnectionManager = connectionManager;
        this.que = que;

        lightPusher.start();
    }

    private void lightPusherFunction() {
        while (!stopLightPusher) {
            this.lightPush(server.getVectorClock());
            try {
                Thread.sleep(ServerConfig.LIGHT_PUSH_DELAY_MILLIS + rand.nextInt(ServerConfig.LIGHT_PUSH_RANDOM_DELAY_MILLIS));
            } catch (InterruptedException e) {
                Thread.currentThread().start();
            }
        }
    }

    //Function call each time I receive a LightPushMsg
    public boolean checkOutOfDate(VectorClock otherVectorClock) {
        boolean outDate = false;
        VectorClock myVectorClock = server.getVectorClock();
        int compareRes = myVectorClock.compareTo(otherVectorClock);
        if(compareRes == VectorClockComparation.CONCURRENT.getCompareResult()
        || compareRes == VectorClockComparation.LESS_THAN.getCompareResult()){
            outDate = true;
        }
        return outDate;
    }

    public List<ClockedData> computeFetch(VectorClock otherVectorClock) {
        VectorClock calcClock = server.getSecondaryIndex().ceilingKey(otherVectorClock);
        //todo null means send me all
        return this.dataManagerReader.recoverData(calcClock);
    }

    public void heavyPush(List<ClockedData> heavy){
        serverConnectionManager.broadcastMessage(new ServerHeavyPushMsg(heavy));
    }

    public void lightPush(VectorClock light){
        serverConnectionManager.broadcastMessage(new ServerLightPushMsg(light));
    }

    public Optional<AbstractMsg> handleHeavyPush(AbstractMsg msg){
        ServerHeavyPushMsg hPush = (ServerHeavyPushMsg) msg;
        this.que.addServerData(hPush.getPayload());
        return Optional.empty();
    }

    public Optional<AbstractMsg> handleLightPush(AbstractMsg msg){
        ServerLightPushMsg lPush = (ServerLightPushMsg) msg;
        return this.checkOutOfDate(lPush.getPayload())?
                Optional.of(new ServerFetchMsg(this.server.getVectorClock())) :
                Optional.empty();
    }

    public Optional<AbstractMsg> fetch(AbstractMsg msg){
        ServerFetchMsg fetch = (ServerFetchMsg) msg;
        List<ClockedData> list = this.computeFetch(fetch.getPayload());
        if(!list.isEmpty())
            return Optional.of(new ServerHeavyPushMsg(list));
        else
            return Optional.empty();
    }

    public void stop() {
        stopLightPusher = true;
    }
}
