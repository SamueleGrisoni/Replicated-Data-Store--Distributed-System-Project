package click.replicatedDataStore.utils.configs;

import click.replicatedDataStore.dataStructures.ServerPorts;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class LoadedLocalServerConfig extends  LoadedConfig{
    public final Boolean isPersistent;
    public final Boolean heavyPropagationPolicy;
    public final Set<Integer> heavyConnections;
    public final Set<Integer> lightConnections;

    public LoadedLocalServerConfig(String name, String ip, ServerPorts ports, Boolean isPersistent,
                                   Boolean heavyPropagationPolicy, Set<Integer> heavyConnections,
                                   Set<Integer> lightConnections) {
        super(name, ip, ports);
        this.isPersistent = isPersistent;
        this.heavyPropagationPolicy = heavyPropagationPolicy;
        this.heavyConnections = Collections.unmodifiableSet(heavyConnections);
        this.lightConnections = Collections.unmodifiableSet(lightConnections);
    }

    @Override
    public String toString() {
        return super.toString() +
                "  Persistent: %s\n".formatted(isPersistent) +
                "  Heavy propagation policy: %s\n".formatted(heavyPropagationPolicy) +
                "  Heavy connections: %s\n".formatted(heavyConnections) +
                "  Light connections: %s\n".formatted(lightConnections);
    }
}
