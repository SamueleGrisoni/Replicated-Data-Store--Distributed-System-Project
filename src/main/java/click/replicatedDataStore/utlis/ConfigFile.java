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

        @JsonProperty("port")
        private int port;

        public String getIp() {
            return ip;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }
    }
}