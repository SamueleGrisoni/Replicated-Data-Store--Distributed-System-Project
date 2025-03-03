package click.replicatedDataStore.connectionLayer.connectionThreads;

import java.io.ObjectInputStream;
import java.net.Socket;

public class ChannelIn extends Thread{
    private final ObjectInputStream inChannels;

    public ChannelIn(ObjectInputStream in){
        this.inChannels = in;
    }

    public void run(){

    }

}
