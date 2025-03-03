package click.replicatedDataStore.connectionLayer.messages;

import click.replicatedDataStore.connectionLayer.CommunicationMethods;

import java.io.Serializable;

public abstract class AbstractMsg<T> implements Serializable{
    public final CommunicationMethods method;
    protected final T payLoad;

    public AbstractMsg(CommunicationMethods method, T payLoad){
        this.method = method;
        this.payLoad = payLoad;
    }

    public abstract T getPayload();
}
