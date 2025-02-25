package click.replicatedDataStore.connectionLayer.messages;

import click.replicatedDataStore.dataStructures.VectorClock;

public class LightPushMsg extends AbstractMsg {
    public final VectorClock payLoad;

    public LightPushMsg(VectorClock payLoad){
        this.payLoad = payLoad;
    }

    @Override
    public String toString() {
        return "LightPushMsg{" +
                payLoad +
                '}';
    }
}
