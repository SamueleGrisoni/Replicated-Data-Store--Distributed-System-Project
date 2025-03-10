package click.replicatedDataStore.connectionLayer.connectionThreads;

import click.replicatedDataStore.connectionLayer.connectionManagers.ConnectionManager;
import click.replicatedDataStore.connectionLayer.messages.AbstractMsg;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Objects;
import java.util.Optional;

public abstract class ConnectionHandler extends Thread{
    protected final ObjectOutputStream out;
    protected final ObjectInputStream in;
    protected final ConnectionManager manager;
    protected boolean running = true;

    protected ConnectionHandler(Socket socket, ConnectionManager manager) throws IOException{
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.in = new ObjectInputStream(socket.getInputStream());
        this.manager = manager;
    }

    public void run(){
        while(running){
            try {
                Object readObj = this.in.readObject();
                AbstractMsg<?> request = (AbstractMsg<?>) readObj;
                Thread response = new Thread(() -> {
                    Optional<AbstractMsg<?>> retMsg = manager.resolveRequest(request);
                    if(retMsg.isPresent()) {
                        try {
                            out.writeObject(retMsg.get());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
                response.start();
            }catch (IOException e){
                if(running)
                    manager.logger.logErr(this.getClass(), "error while processing a request\n" + e.getMessage());
            }catch (ClassNotFoundException e){
                if(running)
                    manager.logger.logErr(this.getClass(), "error the input from the socket isn't an AbstractMsg\n" + e.getMessage());
            }
        }
    }

    protected void stopRunning(){
        running = false;
    }
}
