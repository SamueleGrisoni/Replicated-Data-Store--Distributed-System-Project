package click.replicatedDataStore.connectionLayer.messages;

import click.replicatedDataStore.dataStructures.ClockedData;

import java.time.Clock;
import java.util.List;

public class ServerHeavyPushMsg extends AbstractMsg {

    public ServerHeavyPushMsg(List<ClockedData> clockedData){
        super(null, null);
    }
}
