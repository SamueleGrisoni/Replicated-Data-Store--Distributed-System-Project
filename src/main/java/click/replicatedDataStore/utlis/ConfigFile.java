package click.replicatedDataStore.utlis;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class ConfigFile{
    @JsonProperty("index")
    private int index;

    @JsonProperty("addresses")
    private Map<Integer, ConfigFileEntry> addresses;

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public Map<Integer, ConfigFileEntry> getAddresses() {
        return addresses;
    }

    public void setAddresses(Map<Integer, ConfigFileEntry> addresses) {
        this.addresses = addresses;
    }

    public static class ConfigFileEntry {
        @JsonProperty("address")
        private String ip;

        @JsonProperty("port")
        private int port;

        public String getIp() {
            return ip;
        }

        public void setIp(String address) {
            this.ip = address;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

    }
}