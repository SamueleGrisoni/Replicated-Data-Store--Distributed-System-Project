package click.replicatedDataStore.connectionLayer.messages;

import click.replicatedDataStore.connectionLayer.CommunicationMethods;
import click.replicatedDataStore.connectionLayer.connectionThreads.ServerHandler;

public class ServerIndexMsg extends AbstractMsg<Integer>{

    public ServerIndexMsg(Integer index){
        super(CommunicationMethods.SERVER_INDEX, index);
    }

    @Override
    public Integer getPayload() {
        return payLoad;
    }
}
