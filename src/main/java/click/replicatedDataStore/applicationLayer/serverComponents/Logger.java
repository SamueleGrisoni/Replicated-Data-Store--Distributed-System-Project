package click.replicatedDataStore.applicationLayer.serverComponents;

import click.replicatedDataStore.applicationLayer.Server;
import click.replicatedDataStore.utils.configs.ServerConfig;

public class Logger {
    private final String RESET = "\u001B[0m";
    private final String YELLOW = "\u001B[33m";
    private final String RED = "\u001B[31m";
    private final String WHITE = "\u001B[37m";
    private final Server server;

    public Logger(Server server){
        this.server = server;
    }

    /**
     * Logs an error message in red.
     * @param classSource the source of the class
     * @param msg the error message to log
     */
    public void logErr(Object classSource, String msg) {
        if (ServerConfig.debug) {
            throw new RuntimeException(YELLOW + "SERVER-" + server.getServerName() + ": "+ RED + "ERROR: source: " + classSource + "\n" + msg + RESET);
        } else {
            // Log this on the view with some generic error message
            System.out.println(YELLOW + "SERVER-" + server.getServerName() + ": " + RED + "ERROR: source: " + classSource + "\n" + msg + RESET);
        }
    }

    public void logInfo(String msg){
        System.out.println(YELLOW + "SERVER-" + server.getServerName() + ": " + WHITE + msg + RESET);
    }
}