package click.replicatedDataStore.connectionLayer.messages;

import click.replicatedDataStore.connectionLayer.CommunicationMethods;
import click.replicatedDataStore.utlis.Key;

import java.io.Serializable;

public class ClientReadMsg extends AbstractMsg{

    /**
     * @param key to search
     */
    public ClientReadMsg(Key key){
        super(CommunicationMethods.CLIENT_READ, key);
    }

    public Key getPayload(){
        return (Key) payLoad;
    }
}
