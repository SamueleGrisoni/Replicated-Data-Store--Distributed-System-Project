package click.replicatedDataStore.dataStructures;

public record ServerPorts(Integer incomingPort, Integer outgoingPort) {
    public ServerPorts(Integer incomingPort, Integer outgoingPort) {
        this.incomingPort = incomingPort;
        this.outgoingPort = outgoingPort;
        if(incomingPort == null || outgoingPort == null){
            throw new IllegalArgumentException("Server ports cannot be null");
        }
    }
}
