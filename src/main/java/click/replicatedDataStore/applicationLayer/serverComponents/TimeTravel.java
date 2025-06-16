package click.replicatedDataStore.applicationLayer.serverComponents;

import click.replicatedDataStore.applicationLayer.serverComponents.dataManager.DataManagerReader;
import click.replicatedDataStore.applicationLayer.serverComponents.dataManager.DataManagerWriter;
import click.replicatedDataStore.applicationLayer.serverComponents.dataManager.VectorClockComparation;
import click.replicatedDataStore.connectionLayer.connectionManagers.ServerConnectionManager;
import click.replicatedDataStore.connectionLayer.messages.AbstractMsg;
import click.replicatedDataStore.connectionLayer.messages.ServerFetchMsg;
import click.replicatedDataStore.connectionLayer.messages.ServerHeavyPushMsg;
import click.replicatedDataStore.connectionLayer.messages.ServerLightPushMsg;
import click.replicatedDataStore.dataStructures.ClockedData;
import click.replicatedDataStore.dataStructures.VectorClock;
import click.replicatedDataStore.utils.Key;
import click.replicatedDataStore.utils.configs.ServerConfig;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TimeTravel {
    private final ServerDataSynchronizer serverDataSynchronizer;
    private ServerConnectionManager serverConnectionManager;
    private final DataManagerReader dataManagerReader;
    private final DataManagerWriter dataManagerWriter;
    private final ScheduledExecutorService lightPusher = Executors.newScheduledThreadPool(1);
    private boolean stopLightPusher = false;

    public TimeTravel(ServerDataSynchronizer serverDataSynchronizer, DataManagerReader dataManagerReader, DataManagerWriter dataManagerWriter) {
        this.serverDataSynchronizer = serverDataSynchronizer;
        this.dataManagerReader = dataManagerReader;
        this.dataManagerWriter = dataManagerWriter;

        this.startLightPusher();
    }

    private void startLightPusher(){
        Random rand = new Random();
        int delay = ServerConfig.LIGHT_PUSH_DELAY_MILLIS + rand.nextInt(ServerConfig.LIGHT_PUSH_RANDOM_DELAY_MILLIS);
        lightPusher.scheduleAtFixedRate(this::lightPusherFunction, delay, delay, TimeUnit.MILLISECONDS);
    }

    public void setServerConnectionManager(ServerConnectionManager serverConnectionManager) {
        this.serverConnectionManager = serverConnectionManager;
    }

    private void lightPusherFunction() {
        if (!stopLightPusher) {
            this.lightPush(serverDataSynchronizer.getVectorClock());
        }
    }

    //Function call each time I receive a LightPushMsg
    public boolean checkOutOfDate(VectorClock otherVectorClock) {
        boolean outDate = false;
        VectorClock myVectorClock = serverDataSynchronizer.getVectorClock();
        int compareRes = myVectorClock.compareTo(otherVectorClock);
        if(compareRes == VectorClockComparation.CONCURRENT.getCompareResult()
        || compareRes == VectorClockComparation.LESS_THAN.getCompareResult()){
            outDate = true;
        }
//        if(outDate){
//            System.out.println("Server " + serverDataSynchronizer.getServerIndex() + " is out of date. My clock: " + myVectorClock + " Other clock: " + otherVectorClock);
//        }else{
//            System.out.println("Server " + serverDataSynchronizer.getServerIndex() + " is up to date. My clock: " + myVectorClock + " Other clock: " + otherVectorClock);
//        }
        return outDate;
    }

    public List<ClockedData> computeFetch(VectorClock otherVectorClock) {
        TreeMap<VectorClock, Key> secInd = serverDataSynchronizer.getSecondaryIndex();
        VectorClock calcClock = null;
        List<ClockedData> recovery = new ArrayList<>();
        for (VectorClock thisClock : secInd.descendingKeySet()) {
            if (thisClock.compareTo(otherVectorClock) <= 0)
                break;
            else
                calcClock = thisClock;
        }
        if (calcClock != null)
            recovery = this.dataManagerReader.recoverData(calcClock);
        return recovery;
    }

    public void heavyPush(List<ClockedData> heavy){
        serverConnectionManager.broadcastMessage(new ServerHeavyPushMsg(heavy));
    }

    public void lightPush(VectorClock light){
        serverConnectionManager.broadcastMessage(new ServerLightPushMsg(light));
    }

    public Optional<AbstractMsg<?>> handleHeavyPush(ServerHeavyPushMsg hPush){
        dataManagerWriter.addServerData(hPush.getPayload());
        return Optional.empty();
    }

    public Optional<AbstractMsg<?>> handleLightPush(ServerLightPushMsg lPush){
        return this.checkOutOfDate(lPush.getPayload())?
                Optional.of(new ServerFetchMsg(this.serverDataSynchronizer.getVectorClock())) :
                Optional.empty();
    }

    public Optional<AbstractMsg<?>> handleFetch(ServerFetchMsg fetch){
        //System.out.println("Computing fetch for VectorClock: " + fetch.getPayload());
        List<ClockedData> list = this.computeFetch(fetch.getPayload());
        //todo check this out, return list after server restart when an update as occurred is empty (it should contain the last update)
        //System.out.println("Fetch computed, returning " + list.size() + " ClockedData");
        if(!list.isEmpty())
            return Optional.of(new ServerHeavyPushMsg(list));
        else
            return Optional.empty();
    }

    public void stop() {
        stopLightPusher = true;
        lightPusher.shutdownNow();
    }
}
