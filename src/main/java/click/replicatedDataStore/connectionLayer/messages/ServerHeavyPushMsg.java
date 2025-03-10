package click.replicatedDataStore.connectionLayer.messages;

import click.replicatedDataStore.connectionLayer.CommunicationMethods;
import click.replicatedDataStore.dataStructures.ClockedData;

import java.io.Serializable;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

public class ServerHeavyPushMsg extends AbstractMsg<List<ClockedData>> {

    public ServerHeavyPushMsg(List<ClockedData> clockedData){
        super(CommunicationMethods.SERVER_H_PUSH, new ArrayList<ClockedData>(clockedData));
    }

    public List<ClockedData> getPayload(){
        return  payLoad;
    }
}
