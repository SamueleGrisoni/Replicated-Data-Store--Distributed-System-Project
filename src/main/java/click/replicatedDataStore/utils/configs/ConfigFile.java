package click.replicatedDataStore.utils.configs;

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
        @JsonProperty("serverName")
        private String serverName;

        @JsonProperty("address")
        private String ip;

        @JsonProperty("serverPort")
        private int serverPort;

        @JsonProperty("clientPort")
        private int clientPort;

        @JsonProperty
        private Boolean isPersistent;

        public String getServerName(){
            return serverName;
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

        public Boolean isPersistent() {
            return isPersistent;
        }

        public void setIsPersistent(Boolean isPersistent) {
            this.isPersistent = isPersistent;
        }

        public ConfigFileEntry(){
            // Default constructor for deserialization
        }

        //deep copy constructor
        public ConfigFileEntry(ConfigFileEntry entry){
            this.serverName = entry.serverName;
            this.ip = entry.ip;
            this.serverPort = entry.serverPort;
            this.clientPort = entry.clientPort;
            this.isPersistent = entry.isPersistent;
        }

        @Override
        public String toString() {
            return "ConfigFileEntry{" +
                    "serverName=" + serverName +
                    ", ip='" + ip + '\'' +
                    ", serverPort=" + serverPort +
                    ", clientPort=" + clientPort +
                    ", isPersistent=" + isPersistent +
                    '}';
        }
    }
}