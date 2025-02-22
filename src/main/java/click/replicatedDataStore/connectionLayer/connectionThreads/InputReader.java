package click.replicatedDataStore.connectionLayer.connectionThreads;

import click.replicatedDataStore.connectionLayer.connectionManagers.ConnectionManager;

import java.io.ObjectInputStream;

public class InputReader extends Thread {
    ObjectInputStream in;
    ConnectionManager manager;

    InputReader(ObjectInputStream in, ConnectionManager manager) {
        this.in = in;
        this.manager = manager;
    }

    public void run() {
    }
}
