package click.replicatedDataStore.connectionLayer.connectionManagers;

import click.replicatedDataStore.connectionLayer.connectionThreads.ConnectionAcceptor;

import java.util.HashMap;

public abstract class ConnectionManager {
    private ConnectionAcceptor connectionAcceptor;
    public abstract HashMap<String, Runnable> getRoutes();
}
