package click.replicatedDataStore.applicationLayer.clientComponents.view;

import click.replicatedDataStore.utlis.ClientGlobalParameters;

public class ClientErrorManager {

    public ClientErrorManager(){
    }

    /**
     * @param classSource the source of the class
     * @param msg message of the error to manage
     */
    public void logErr(Object classSource, String msg) {
        if(ClientGlobalParameters.debug){
            throw new RuntimeException("source: " + classSource + "\n" + "\n" + msg);
        }else{
            //todo log this on the view with some generic error message
            System.out.println("error");
        }
    }
}
