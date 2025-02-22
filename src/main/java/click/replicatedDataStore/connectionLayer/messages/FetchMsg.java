package click.replicatedDataStore.connectionLayer.messages;

import click.replicatedDataStore.applicationLayer.serverComponents.VectorClock;

public class FetchMsg extends AbstractMsg {
    public final VectorClock payLoad;

    public FetchMsg(VectorClock payLoad){
        this.payLoad = payLoad;
    }
}
