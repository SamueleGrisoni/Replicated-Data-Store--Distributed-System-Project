package click.replicatedDataStore.applicationLayer.serverComponents.dataManager;

import click.replicatedDataStore.applicationLayer.Server;
import click.replicatedDataStore.connectionLayer.messages.AnswerState;
import click.replicatedDataStore.utlis.Key;

import java.io.Serializable;

public class DataManagerReader {
    private final Server server;

    //TODO do we really need this???
    public DataManagerReader(Server server) {
        this.server = server;
    }

    public Serializable read(Key key) {
        //TODO implement read
        return null;
    }

}
