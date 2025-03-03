package click.replicatedDataStore.connectionLayer.messages;

import click.replicatedDataStore.connectionLayer.CommunicationMethods;
import click.replicatedDataStore.dataStructures.ClientWrite;
import click.replicatedDataStore.utlis.Key;

import java.io.Serializable;

public class ClientWriteMsg extends AbstractMsg<ClientWrite>{
    /**
     * @param clientWrite the client write to package
     */
    public ClientWriteMsg(ClientWrite clientWrite){
        super(CommunicationMethods.CLIENT_WRITE, clientWrite);
    }

    public ClientWrite getPayload(){
        return (ClientWrite) this.payLoad;
    }
}
