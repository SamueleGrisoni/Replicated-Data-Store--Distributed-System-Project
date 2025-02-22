package click.replicatedDataStore.connectionLayer.messages;

import click.replicatedDataStore.utlis.Key;

public class ReadMsg extends AbstractMsg{
    public final Key key;

    public ReadMsg(Key key){
        this.key = key;
    }
}
