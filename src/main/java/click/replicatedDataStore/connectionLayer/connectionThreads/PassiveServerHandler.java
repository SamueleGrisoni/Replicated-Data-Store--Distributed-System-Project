package click.replicatedDataStore.connectionLayer.connectionThreads;

import click.replicatedDataStore.connectionLayer.connectionManagers.ServerConnectionManager;
import click.replicatedDataStore.connectionLayer.messages.AnswerState;
import click.replicatedDataStore.connectionLayer.messages.ServerIndexMsg;
import click.replicatedDataStore.connectionLayer.messages.StateAnswerMsg;

import java.io.IOException;
import java.net.Socket;

public class PassiveServerHandler extends ServerHandler{
    public PassiveServerHandler(Socket serverSocket, ServerConnectionManager connectionManager) throws IOException, ConnectionCreateTimeOutException {
        super(serverSocket, connectionManager);
        Runnable setupConnection = new Thread(() -> {
            ServerIndexMsg indexMsg;
            try {
                indexMsg = (ServerIndexMsg) in.readObject();
                connectionManager.addIndexed(indexMsg.getPayload(), this);
                super.connectionEstablished = true;
                out.writeObject(new StateAnswerMsg(AnswerState.OK));
            } catch (Exception e) { //catch both ClassNotFound and IOException by readObject
                windDown();
                manager.logger.logErr(this.getClass(), "error: didnt' receive initialization message with server index\n" + e.getMessage());
            }
            synchronized (notify) {
                notify.notifyAll();
            }
        });
        super.createConnectionTimed(setupConnection, "PassiveServerHandler: corrupted message");
    }

    private void windDown(){
        this.running = false;
        try {
            in.close();
            out.close();
        } catch (IOException ex) {
            this.running = false;
            throw new RuntimeException("error: can't close input or output stream"+ ex);
        }
    }
}
