package click.replicatedDataStore.utils.configs;

import click.replicatedDataStore.dataStructures.ServerPorts;

import java.util.List;
import java.util.Optional;

public class LoadedConfig {
    public final String serverName;
    public final String ip;
    public final ServerPorts ports;

    public LoadedConfig(String serverName, String ip, ServerPorts ports) {
        this.serverName = serverName;
        this.ip = ip;
        this.ports = ports;
    }

    @Override
    public String toString() {
        return "Server %s at %s (Server port: %d, Client port: %d)\n".formatted(
                serverName, ip, ports.serverPort(), ports.clientPort());
    }
}
