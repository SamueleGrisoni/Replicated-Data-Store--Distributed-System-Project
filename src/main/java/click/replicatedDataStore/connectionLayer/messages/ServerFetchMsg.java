package click.replicatedDataStore.connectionLayer.messages;

import click.replicatedDataStore.connectionLayer.CommunicationMethods;
import click.replicatedDataStore.dataStructures.Pair;
import click.replicatedDataStore.dataStructures.VectorClock;

public class ServerFetchMsg extends AbstractMsg<Pair<VectorClock, Integer>> {

    public ServerFetchMsg(VectorClock vectorClock, int serverIndex){
        super(CommunicationMethods.SERVER_FETCH, new Pair<>(vectorClock, serverIndex));
    }

    public Pair<VectorClock, Integer> getPayload(){
        return payLoad;
    }
}
