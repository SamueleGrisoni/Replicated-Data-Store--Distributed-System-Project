package click.replicatedDataStore.connectionLayer.messages;

import click.replicatedDataStore.applicationLayer.serverComponents.VectorClock;

public class LightPushMsg extends AbstractMsg {
    public final VectorClock payLoad;

    public LightPushMsg(VectorClock payLoad){
        this.payLoad = payLoad;
    }
}
