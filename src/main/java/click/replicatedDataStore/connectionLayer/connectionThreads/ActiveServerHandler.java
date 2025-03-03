package click.replicatedDataStore.connectionLayer.connectionThreads;

import click.replicatedDataStore.connectionLayer.connectionManagers.ServerConnectionManager;
import click.replicatedDataStore.connectionLayer.messages.ServerIndexMsg;

import java.io.IOException;
import java.net.Socket;

public class ActiveServerHandler extends ServerHandler{
    public ActiveServerHandler(Socket socket, ServerConnectionManager connectionManager, Integer index) throws IOException {
        super(socket, connectionManager);
        try {
            this.out.writeObject(new ServerIndexMsg(index));
            connectionManager.addIndexed(index, this);
        }catch (IOException e){
            manager.logger.logErr(this.getClass(), "error: unable to send initialization message with server index\n" + e.getMessage());
            in.close();
            out.close();
        }
        connectionManager.addIndexed(index, this);
    }
}
