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
import click.replicatedDataStore.utlis.Key;
import click.replicatedDataStore.utlis.configs.ServerConfig;

import java.util.*;

public class TimeTravel {
    private final ServerDataSynchronizer serverDataSynchronizer;
    private ServerConnectionManager serverConnectionManager;
    private final DataManagerReader dataManagerReader;
    private final DataManagerWriter dataManagerWriter;
    private final Thread lightPusher;
    private boolean stopLightPusher = false;
    private final Random rand = new Random();

    public TimeTravel(ServerDataSynchronizer serverDataSynchronizer, DataManagerReader dataManagerReader, DataManagerWriter dataManagerWriter) {
        this.serverDataSynchronizer = serverDataSynchronizer;
        this.dataManagerReader = dataManagerReader;
        this.dataManagerWriter = dataManagerWriter;
        this.lightPusher = new Thread(this::lightPusherFunction);
        lightPusher.start();
    }

    public void setServerConnectionManager(ServerConnectionManager serverConnectionManager) {
        this.serverConnectionManager = serverConnectionManager;
    }

    private void lightPusherFunction() {
        while (!stopLightPusher) {
            try {
                Thread.sleep(ServerConfig.LIGHT_PUSH_DELAY_MILLIS + rand.nextInt(ServerConfig.LIGHT_PUSH_RANDOM_DELAY_MILLIS));
            } catch (InterruptedException e) {
                if(!stopLightPusher)
                    Thread.currentThread().start();
            }
            if(!stopLightPusher)
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
        List<ClockedData> list = this.computeFetch(fetch.getPayload());
        if(!list.isEmpty())
            return Optional.of(new ServerHeavyPushMsg(list));
        else
            return Optional.empty();
    }

    public void stop() {
        stopLightPusher = true;
        lightPusher.interrupt();
    }
}
