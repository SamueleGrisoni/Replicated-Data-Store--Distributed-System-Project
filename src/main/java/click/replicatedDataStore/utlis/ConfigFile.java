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

        @JsonProperty("serverPort")
        private int serverPort;

        @JsonProperty("clientPort")
        private int clientPort;

        public int getServerPort() {
            return serverPort;
        }

        public int getClientPort() {
            return clientPort;
        }

        public String getIp() {
            return ip;
        }
    }
}