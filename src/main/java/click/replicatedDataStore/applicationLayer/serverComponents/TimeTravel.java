package click.replicatedDataStore.applicationLayer.serverComponents;

import click.replicatedDataStore.applicationLayer.Server;
import click.replicatedDataStore.applicationLayer.serverComponents.dataManager.DataManagerReader;
import click.replicatedDataStore.applicationLayer.serverComponents.dataManager.VectorClockComparation;
import click.replicatedDataStore.connectionLayer.connectionManagers.ServerConnectionManager;
import click.replicatedDataStore.dataStructures.ClockedData;
import click.replicatedDataStore.dataStructures.VectorClock;
import click.replicatedDataStore.utlis.Key;
import click.replicatedDataStore.utlis.ServerConfig;

import java.util.*;

public class TimeTravel {
    private final Server server;
    private final ServerConnectionManager serverConnectionManager;
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

    public void stop() {
        stopLightPusher = true;
    }
}
