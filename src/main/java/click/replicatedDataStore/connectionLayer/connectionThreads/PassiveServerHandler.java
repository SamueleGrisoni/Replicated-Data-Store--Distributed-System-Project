package click.replicatedDataStore.connectionLayer.connectionThreads;

import click.replicatedDataStore.connectionLayer.connectionManagers.ServerConnectionManager;
import click.replicatedDataStore.connectionLayer.messages.AnswerState;
import click.replicatedDataStore.connectionLayer.messages.ServerIndexMsg;
import click.replicatedDataStore.connectionLayer.messages.StateAnswerMsg;

import java.io.IOException;
import java.net.Socket;

public class PassiveServerHandler extends ServerHandler{
    public PassiveServerHandler(Socket serverSocket, ServerConnectionManager connectionManager) throws IOException {
        super(serverSocket, connectionManager);
        ServerIndexMsg indexMsg;
        try {
            indexMsg = (ServerIndexMsg) in.readObject(); //todo add a timer and interrupt after x time
            connectionManager.addIndexed(indexMsg.getPayload(), this);
            out.writeObject(new StateAnswerMsg(AnswerState.OK));
        } catch (Exception e) { //catch both ClassNotFound and IOException by readObject
            manager.logger.logErr(this.getClass(), "error: didnt' receive initialization message with server index\n" + e.getMessage());
            in.close();
            out.close();
            this.running = false;
        }
    }
}
