package click.replicatedDataStore.utlis;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ConfigFile{
    @JsonProperty("servers")
    private List<ConfigFileEntry> servers;

    public List<ConfigFileEntry> getServers() {
        return servers;
    }

    public static class ConfigFileEntry {

        @JsonProperty("address")
        private String ip;

        @JsonProperty("incomingPort")
        private int incomingPort;

        @JsonProperty("outgoingPort")
        private int outgoingPort;

        public int getIncomingPort() {
            return incomingPort;
        }

        public int getOutgoingPort() {
            return outgoingPort;
        }

        public String getIp() {
            return ip;
        }
    }
}