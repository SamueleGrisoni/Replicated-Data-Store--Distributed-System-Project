package click.replicatedDataStore.connectionLayer.connectionThreads;

import click.replicatedDataStore.connectionLayer.connectionManagers.ServerConnectionManager;
import click.replicatedDataStore.connectionLayer.messages.AnswerState;
import click.replicatedDataStore.connectionLayer.messages.ServerIndexMsg;
import click.replicatedDataStore.connectionLayer.messages.StateAnswerMsg;

import java.io.IOException;
import java.net.Socket;

public class ActiveServerHandler extends ServerHandler{
    public ActiveServerHandler(Socket socket, ServerConnectionManager connectionManager, Integer localServerIndex,
                               Integer contactedServerIndex) throws IOException, ConnectionCreateTimeOutException {
        super(socket, connectionManager);
        Runnable setupConnection = new Thread(() ->{
            try {
                this.out.writeObject(new ServerIndexMsg(localServerIndex));
                AnswerState answer = ((StateAnswerMsg) this.in.readObject()).getPayload();
                if (answer == AnswerState.OK) {
                    super.connectionEstablished = true;
                    connectionManager.addIndexed(contactedServerIndex, this);
                }else
                    throw new Error("error: failing answer from contacted passive handler");
            } catch (IOException e) {
                windDown();
                manager.logger.logErr(this.getClass(), "error: unable to send initialization message with " +
                        "server index\n" + e.getMessage());
            } catch (ClassNotFoundException e) {
                windDown();
                manager.logger.logErr(this.getClass(), "error: unable to read answer state from " +
                        "passive\n" + e.getMessage());
            }
            synchronized (notify) {
                notify.notifyAll();
            }
        });
        super.createConnectionTimed(setupConnection, "ActiveServerHandler: did not receive index msg");
    }

    private void windDown(){
        this.running = false;
        try {
            in.close();
            out.close();
        } catch (IOException ex) {
            throw new RuntimeException("error: can't close input or output stream"+ ex);
        }
    }
}
