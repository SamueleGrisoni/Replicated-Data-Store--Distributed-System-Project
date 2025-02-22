package click.replicatedDataStore.connectionLayer.messages;

import click.replicatedDataStore.dataStructures.ClockedData;

import java.util.List;

public class HeavyPushMsg extends AbstractMsg {
    public final List<ClockedData> payLoad;

    public HeavyPushMsg(List<ClockedData> payLoad){
        this.payLoad = payLoad;
    }
}
