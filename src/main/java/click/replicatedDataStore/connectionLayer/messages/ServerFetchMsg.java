package click.replicatedDataStore.connectionLayer.messages;

import click.replicatedDataStore.dataStructures.VectorClock;

public class ServerFetchMsg extends AbstractMsg {

    public ServerFetchMsg(VectorClock vectorClock){
        super(null, null);
    }
}
