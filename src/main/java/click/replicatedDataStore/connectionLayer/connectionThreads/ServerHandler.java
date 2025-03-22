package click.replicatedDataStore.connectionLayer.connectionThreads;

import click.replicatedDataStore.connectionLayer.connectionManagers.ServerConnectionManager;
import click.replicatedDataStore.connectionLayer.messages.AbstractMsg;
import click.replicatedDataStore.utlis.configs.ServerConfig;
import java.io.IOException;
import java.io.NotSerializableException;
import java.net.Socket;

public abstract class ServerHandler extends ConnectionHandler{
    protected final Object notify = new Object();
    protected Boolean connectionEstablished = false;
    public ServerHandler(Socket serverSocket, ServerConnectionManager manager) throws IOException {
        super(serverSocket, manager);
    }

    protected void createConnectionTimed(Runnable establishingConnectionThread, String errorMsg) {
        Thread task = new Thread(establishingConnectionThread);
        task.start();
        synchronized (notify){
            try {
                notify.wait(ServerConfig.otherServerResponseWaitMilliseconds);
            } catch (InterruptedException ignored) {}
        }
        if(!connectionEstablished){
            task.interrupt();
            throw new ConnectionCreateTimeOutException(errorMsg);
        }
    }

    public void sendMessage(AbstractMsg<?> msg){
        Thread t = new Thread(() -> {
            try {
                out.writeObject(msg);
            } catch (NotSerializableException notSer){
                manager.logger.logErr(this.getClass(), "error: message not serializable" + notSer.getMessage());
            } catch (IOException e) {
                manager.logger.logErr(this.getClass(), "error: sending message" + e.getMessage());
            }
        });
        t.start();
    }

    public void stopRunning(){
        running = false;
        try {
            out.close();
            in.close();
        } catch (IOException e){
            manager.logger.logErr(this.getClass(), "error while closing client connection\n" + e.getMessage());
        }
    }
}
