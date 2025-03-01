package click.replicatedDataStore.connectionLayer.messages;

import click.replicatedDataStore.connectionLayer.CommunicationMethods;
import click.replicatedDataStore.dataStructures.ClientWrite;
import click.replicatedDataStore.utlis.Key;

import java.io.Serializable;

public class ClientWriteMsg extends AbstractMsg{
    /**
     * @param key used to index
     * @param value the value to store
     */
    public ClientWriteMsg(Key key, Serializable value){
        super(CommunicationMethods.CLIENT_WRITE, new ClientWrite(key, value));
    }

    public ClientWrite getPayload(){
        return (ClientWrite) this.payLoad;
    }
}
