package click.replicatedDataStore.applicationLayer.serverComponents;

import click.replicatedDataStore.applicationLayer.Server;
import click.replicatedDataStore.applicationLayer.serverComponents.dataManager.DataManagerReader;
import click.replicatedDataStore.applicationLayer.serverComponents.dataManager.VectorClockComparation;
import click.replicatedDataStore.connectionLayer.connectionManagers.ServerConnectionManager;
import click.replicatedDataStore.connectionLayer.messages.ServerFetchMsg;
import click.replicatedDataStore.connectionLayer.messages.ServerHeavyPushMsg;
import click.replicatedDataStore.connectionLayer.messages.ServerLightPushMsg;
import click.replicatedDataStore.dataStructures.ClockedData;
import click.replicatedDataStore.dataStructures.VectorClock;
import click.replicatedDataStore.utlis.ClockTooFarAhead;
import click.replicatedDataStore.utlis.ServerConfig;
import click.replicatedDataStore.utlis.Key;

import java.io.Serializable;
import java.util.*;

public class TimeTravel {
    private final Server server;
    private ServerConnectionManager serverConnectionManager;
    private final DataManagerReader dataManagerReader;
    private final Thread lightPusher;
    private boolean stopLightPusher = false;
    private final Random rand = new Random();
    private final Logger logger;

    public TimeTravel(Server server, DataManagerReader dataManagerReader,
                      ServerConnectionManager connectionManager, Logger logger) {
        this.server = server;
        this.dataManagerReader = dataManagerReader;
        this.lightPusher = new Thread(this::lightPusherFunction);
        this.serverConnectionManager = connectionManager;
        this.logger = logger;

        lightPusher.start();
    }

    private void lightPusherFunction() {
        while (!stopLightPusher) {
            serverConnectionManager.lightPush(server.getVectorClock());
            try {
                Thread.sleep(ServerConfig.LIGHT_PUSH_DELAY_MILLIS + rand.nextInt(ServerConfig.RANDOM_DELAY));
            } catch (InterruptedException e) {
                Thread.currentThread().start();
            }
        }
    }

    //Function call each time I receive a LightPushMsg
    public void checkMySync(VectorClock otherVectorClock) {
        VectorClock myVectorClock = server.getVectorClock();
        int compareRes = myVectorClock.compareTo(otherVectorClock);
        if(compareRes == VectorClockComparation.CONCURRENT.getCompareResult()
        || compareRes == VectorClockComparation.LESS_THAN.getCompareResult()){
            serverConnectionManager.fetch(myVectorClock);
        }
    }

    public List<ClockedData> computeFetch(VectorClock otherVectorClock) {
        return this.dataManagerReader.recoverData(server.getSecondaryIndex().ceilingKey(otherVectorClock));
    }

    public void stopLightPusher() {
        stopLightPusher = true;
    }
}
