package click.replicatedDataStore.connectionLayer.messages;

import click.replicatedDataStore.connectionLayer.CommunicationMethods;
import click.replicatedDataStore.dataStructures.VectorClock;

public class ServerLightPushMsg extends AbstractMsg<VectorClock> {

    public ServerLightPushMsg(VectorClock vectorClock){
        super(CommunicationMethods.SERVER_L_PUSH, vectorClock);
    }

    public VectorClock getPayload(){
        return (VectorClock) payLoad;
    }
}
