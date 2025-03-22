package click.replicatedDataStore.applicationLayer;

import click.replicatedDataStore.ServerInitializer;
import click.replicatedDataStore.utlis.configs.ServerConfig;
import click.replicatedDataStore.utlis.serverUtilis.ServerInitializerUtils;

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
     * Logs an informational message in yellow.
     * @param classSource the source of the class
     * @param msg the informational message to log
     */
    public void logDebug(Object classSource, String msg) {
        System.out.println(YELLOW + "INFO: source: " + classSource + "\n" + msg + RESET);
    }

    /**
     * Logs an error message in red.
     * @param classSource the source of the class
     * @param msg the error message to log
     */
    public void logErr(Object classSource, String msg) {
        if (ServerConfig.debug) {
            throw new RuntimeException(RED + "ERROR: source: " + classSource + "\n" + msg + RESET);
        } else {
            // Log this on the view with some generic error message
            System.out.println(RED + "ERROR: source: " + classSource + "\n" + msg + RESET);
        }
    }

    public void logInfo(String msg){
        System.out.println(WHITE + "SERVER " + ServerInitializerUtils.getServerIdFromIndex(this.server.getServerIndex()) + ":" + msg + RESET);
    }
}