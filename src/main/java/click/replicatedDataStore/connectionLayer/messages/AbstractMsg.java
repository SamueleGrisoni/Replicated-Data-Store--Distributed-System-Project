package click.replicatedDataStore.connectionLayer.messages;

import click.replicatedDataStore.connectionLayer.CommunicationMethods;

import java.io.Serializable;

public abstract class AbstractMsg implements Serializable{
    public final CommunicationMethods method;
    protected final Serializable payLoad;

    public AbstractMsg(CommunicationMethods method, Serializable payLoad){
        this.method = method;
        this.payLoad = payLoad;
    }
}
