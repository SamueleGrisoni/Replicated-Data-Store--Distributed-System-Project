package click.replicatedDataStore.connectionLayer.messages;

import click.replicatedDataStore.dataStructures.VectorClock;

public class ServerLightPushMsg extends AbstractMsg {

    public ServerLightPushMsg(VectorClock vectorClock){
        super(null, null);
    }

    @Override
    public String toString() {
        return "LightPushMsg{" +
                payLoad +
                '}';
    }
}
