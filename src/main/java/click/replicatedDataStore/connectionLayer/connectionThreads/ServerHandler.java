package click.replicatedDataStore.connectionLayer.connectionThreads;

import click.replicatedDataStore.connectionLayer.connectionManagers.ServerConnectionManager;
import click.replicatedDataStore.connectionLayer.messages.AbstractMsg;
import click.replicatedDataStore.connectionLayer.messages.ServerIndexMsg;

import java.io.IOException;
import java.net.Socket;

public abstract class ServerHandler extends ConnectionHandler{

    public ServerHandler(Socket serverSocket, ServerConnectionManager manager) throws IOException {
        super(serverSocket, manager);
    }

    public void run(){
        while(running){
            try {
                AbstractMsg request = (AbstractMsg) in.readObject();
                out.writeObject(manager.resolveRequest(request));
            }catch (IOException e){
                if(running)
                    manager.logger.logErr(this.getClass(), "error while processing a request\n" + e.getMessage());
            }catch (ClassNotFoundException e){
                if(running)
                    manager.logger.logErr(this.getClass(), "error the input from the socket isn't an AbstractMsg\n" + e.getMessage());
            }
        }
    }

    public void sendMessage(AbstractMsg msg){
        new Thread(() -> {
            try {
                out.writeObject(msg);
            } catch (IOException e) {
                manager.logger.logErr(this.getClass(), "error: sending message" + e.getMessage());
            }
        });
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
