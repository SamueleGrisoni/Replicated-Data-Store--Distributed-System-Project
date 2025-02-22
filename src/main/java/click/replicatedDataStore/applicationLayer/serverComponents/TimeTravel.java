package click.replicatedDataStore.applicationLayer.serverComponents;

import click.replicatedDataStore.applicationLayer.Server;
import click.replicatedDataStore.applicationLayer.serverComponents.DataManager.DataManagerReader;

public class TimeTravel {
    private final Server server;
    //private final ServerConnectionManager serverConnectionManager;
    private final DataManagerReader dataManagerReader;
    private final Thread lightPusher;

    public TimeTravel(Server server, DataManagerReader dataManagerReader) {
        this.server = server;
        this.dataManagerReader = dataManagerReader;
        this.lightPusher = new Thread(this::lightPusherFunction);
    }

    private void lightPusherFunction() {
        //todo
    }

    public void checkMySync(VectorClock otherVectorClock) {
        //todo
    }

    public void helpOtherSync(VectorClock otherVectorClock) {
        //todo
    }
}
