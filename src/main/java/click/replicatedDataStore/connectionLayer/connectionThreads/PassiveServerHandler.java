package click.replicatedDataStore.connectionLayer.connectionThreads;

import click.replicatedDataStore.connectionLayer.connectionManagers.ServerConnectionManager;
import click.replicatedDataStore.connectionLayer.messages.AbstractMsg;
import click.replicatedDataStore.connectionLayer.messages.ServerIndexMsg;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class PassiveServerHandler extends ServerHandler{
    public PassiveServerHandler(Socket serverSocket, ServerConnectionManager connectionManager) throws IOException {
        super(serverSocket, connectionManager);
        ServerIndexMsg indexMsg = null;
        try {
            indexMsg = (ServerIndexMsg) in.readObject(); //todo add a timer and interrupt after x time
            connectionManager.addIndexed(indexMsg.getPayload(), this);
        } catch (Exception e) { //catch both ClassNotFound and IOException by readObject
            manager.logger.logErr(this.getClass(), "error: didnt' receive initialization message with server index\n" + e.getMessage());
            in.close();
            out.close();
        }
    }
}
