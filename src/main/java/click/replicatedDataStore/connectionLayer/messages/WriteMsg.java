package click.replicatedDataStore.connectionLayer.messages;

import click.replicatedDataStore.utlis.Key;

public class WriteMsg extends AbstractMsg{
    public final Key key;
    public final Object value;

    public WriteMsg(Key key, Object value){
        this.key = key;
        this.value = value;
    }
}
