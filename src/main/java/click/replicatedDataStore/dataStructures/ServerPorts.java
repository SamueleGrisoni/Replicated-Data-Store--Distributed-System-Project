package click.replicatedDataStore.dataStructures;

public record ServerPorts(Integer serverPort, Integer clientPort) {
    public ServerPorts(Integer serverPort, Integer clientPort) {
        this.serverPort = serverPort;
        this.clientPort = clientPort;
        if(serverPort == null || clientPort == null){
            throw new IllegalArgumentException("Server ports cannot be null");
        }
    }
}
