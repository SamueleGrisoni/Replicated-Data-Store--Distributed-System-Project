package click.replicatedDataStore.applicationLayer.clientComponents;

import click.replicatedDataStore.applicationLayer.clientComponents.view.ClientErrorManager;
import click.replicatedDataStore.connectionLayer.messages.*;
import click.replicatedDataStore.dataStructures.ClientWrite;
import click.replicatedDataStore.utlis.Key;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;

public class RequestSender {
    private final ClientErrorManager errorManager;
    private final ObjectInputStream in;
    private final ObjectOutputStream out;
    /**
     * @param ip the ip to connect to
     * @param port the port to connect to
     * @param eManager the manager responsible for handling errors
     */
    public RequestSender(String ip, int port, ClientErrorManager eManager) {
        this.errorManager = eManager;

        ObjectInputStream in = null;
        ObjectOutputStream out = null;

        Socket socket = null;
        try {
            socket = new Socket(ip, port);
            in = new ObjectInputStream(socket.getInputStream());
            out = new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            eManager.logErr(this.getClass(),
                        "unable to open socket to " + ip + ":" + port + "\n" +
                             e.getMessage());
        } finally {
            this.in = in;
            this.out = out;
        }
    }

    /**
     * @param key to query
     * @return the Object queried
     */
    public ClientWrite read(Key key) {
        try {
            out.writeObject(new ClientReadMsg(key));
            out.flush();
        }catch (IOException e){
            errorManager.logErr(this.getClass(),
                            "unable to write the object correctly" + "\n" +
                                e.getMessage());
        }

        ClientWrite response = null;
        try {

            response = ((ClientWriteMsg) in.readObject()).getPayload();
        } catch (IOException ioException){
            errorManager.logErr(this.getClass(),
                                "unable to read the object correctly" + "\n" + ioException.getMessage());
        } catch (ClassNotFoundException cException) {
            errorManager.logErr((this.getClass()), "unable to decode correctly the response" + "\n" + cException.getMessage());
        }

        return response;
    }

    /**
     * @param key index for the value to store
     * @param value to store
     * @return the success or failure of the transaction
     */
    public AnswerState write(Key key, Serializable value) {
        try {
            out.writeObject(new ClientWriteMsg(new ClientWrite(key, value
            )));
            out.flush();
        }catch (IOException e){
            errorManager.logErr(this.getClass(),
                    "unable to write the object correctly" + "\n" +
                            e.getMessage());
        }

        AnswerState response = AnswerState.FAIL;
        try {
            response = ((StateAnswerMsg) in.readObject()).getPayload();
        } catch (IOException ioException){
            errorManager.logErr(this.getClass(),
                    "unable to read the object correctly" + "\n" + ioException.getMessage());
        } catch (ClassNotFoundException cException) {
            errorManager.logErr((this.getClass()), "unable to decode correctly the response" + "\n" + cException.getMessage());
        }
        return response;
    }

    public void disconnect(){
        try {
            in.close();
            out.close();
        } catch (IOException e) {
            errorManager.logErr(this.getClass(),
                    "unable to close the connection correctly" + "\n" +
                            e.getMessage());
        }
    }
}
