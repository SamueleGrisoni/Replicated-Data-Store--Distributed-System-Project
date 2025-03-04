package click.replicatedDataStore.connectionLayer.messages;

import click.replicatedDataStore.connectionLayer.CommunicationMethods;
import click.replicatedDataStore.dataStructures.Pair;
import click.replicatedDataStore.dataStructures.VectorClock;

public class ServerFetchMsg extends AbstractMsg<VectorClock> {

    public ServerFetchMsg(VectorClock vectorClock){
        super(CommunicationMethods.SERVER_FETCH, vectorClock);
    }

    public VectorClock getPayload(){
        return payLoad;
    }
}
