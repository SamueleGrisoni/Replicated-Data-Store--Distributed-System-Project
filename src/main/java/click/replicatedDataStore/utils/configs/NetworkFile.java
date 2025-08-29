package click.replicatedDataStore.utils.configs;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class NetworkFile {

    @JsonProperty("overlayNetworkHeavyPush")
    private Map<String, List<String>> overlayNetHeavy;

    @JsonProperty("overlayNetworkLightPush")
    private Map<String, List<String>> overlayNetLight;

    @JsonProperty("propagateHeavePush")
    private boolean propagateHeavyPush;

    public Map<String, List<String>> getHeavyPushOverlayNet(){
        return overlayNetHeavy;
    }

    public Map<String, List<String>> getLightPushOverlayNet(){
        return overlayNetLight;
    }

    public boolean getHeavyPushPropagationPolicy(){
        return propagateHeavyPush;
    }

}
