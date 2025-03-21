package click.replicatedDataStore.utlis;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;

public class ConfigFile{
    @JsonProperty("localServer")
    private List<ConfigFileEntry> localServer;
    @JsonProperty("otherServers")
    private List<ConfigFileEntry> otherServers;

    public List<ConfigFileEntry> getLocalServer() {
        return localServer;
    }

    public List<ConfigFileEntry> getOtherServers() {
        return otherServers;
    }

    public static class ConfigFileEntry implements Serializable {
        @JsonProperty("id")
        private int serverId;

        @JsonProperty("address")
        private String ip;

        @JsonProperty("serverPort")
        private int serverPort;

        @JsonProperty("clientPort")
        private int clientPort;

        public int getServerId(){
            return serverId;
        }

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