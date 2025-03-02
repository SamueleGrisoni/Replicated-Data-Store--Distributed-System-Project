package click.replicatedDataStore.applicationLayer.serverComponents;

import click.replicatedDataStore.ServerGlobalParameters;

public class Logger {
    /**
     * @param classSource the source of the class
     * @param msg message of the error to manage
     */
    public void logErr(Object classSource, String msg) {
        if(ServerGlobalParameters.debug){
            throw new RuntimeException("source: " + classSource + "\n" + "\n" + msg);
        }else{
            //todo log this on the view with some generic error message
            System.out.println("error");
        }
    }
}
