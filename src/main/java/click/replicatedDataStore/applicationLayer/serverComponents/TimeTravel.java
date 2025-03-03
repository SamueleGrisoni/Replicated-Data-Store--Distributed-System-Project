package click.replicatedDataStore.applicationLayer.serverComponents;

import click.replicatedDataStore.applicationLayer.Server;
import click.replicatedDataStore.applicationLayer.serverComponents.dataManager.DataManagerReader;
import click.replicatedDataStore.applicationLayer.serverComponents.dataManager.VectorClockComparation;
import click.replicatedDataStore.connectionLayer.connectionManagers.ServerConnectionManager;
import click.replicatedDataStore.dataStructures.ClockedData;
import click.replicatedDataStore.dataStructures.VectorClock;
import click.replicatedDataStore.applicationLayer.Logger;
import click.replicatedDataStore.utlis.ServerConfig;

import java.util.*;

public class TimeTravel {
    private final Server server;
    private ServerConnectionManager serverConnectionManager;
    private final DataManagerReader dataManagerReader;
    private final Thread lightPusher;
    private boolean stopLightPusher = false;
    private final Random rand = new Random();

    public TimeTravel(Server server, DataManagerReader dataManagerReader,
                      ServerConnectionManager connectionManager) {
        this.server = server;
        this.dataManagerReader = dataManagerReader;
        this.lightPusher = new Thread(this::lightPusherFunction);
        this.serverConnectionManager = connectionManager;

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
