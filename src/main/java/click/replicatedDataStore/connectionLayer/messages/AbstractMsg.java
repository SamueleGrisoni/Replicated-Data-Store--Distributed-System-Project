package click.replicatedDataStore.connectionLayer.messages;

import java.io.Serializable;

public abstract class AbstractMsg implements Serializable{
    protected String method;
    protected Serializable payLoad;
}
