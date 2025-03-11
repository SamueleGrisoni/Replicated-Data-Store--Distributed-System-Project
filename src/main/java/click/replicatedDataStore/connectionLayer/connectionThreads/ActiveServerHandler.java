package click.replicatedDataStore.connectionLayer.connectionThreads;

import click.replicatedDataStore.connectionLayer.connectionManagers.ServerConnectionManager;
import click.replicatedDataStore.connectionLayer.messages.AnswerState;
import click.replicatedDataStore.connectionLayer.messages.ServerIndexMsg;
import click.replicatedDataStore.connectionLayer.messages.StateAnswerMsg;

import java.io.IOException;
import java.net.Socket;

public class ActiveServerHandler extends ServerHandler{
    public ActiveServerHandler(Socket socket, ServerConnectionManager connectionManager, Integer localServerIndex, Integer contactedServerIndex) throws IOException {
        super(socket, connectionManager);
        try {
            this.out.writeObject(new ServerIndexMsg(localServerIndex));
            AnswerState answer = ((StateAnswerMsg)this.in.readObject()).getPayload();
            if(answer == AnswerState.OK)
                connectionManager.addIndexed(contactedServerIndex, this);
            else
                throw new Error("error: failing answer from contacted passive handler");
         }catch (IOException e) {
            manager.logger.logErr(this.getClass(), "error: unable to send initialization message with server index\n" + e.getMessage());
            in.close();
            out.close();
            this.running = false;
        } catch (ClassNotFoundException e) {
            manager.logger.logErr(this.getClass(), "error: unable to read answer state from passive\n" + e.getMessage());
        }
    }
}
