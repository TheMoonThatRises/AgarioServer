package ceccs.network.data;

import org.json.JSONObject;

import java.util.UUID;

public record RegisterPacket(double width, double height, double maxFramerate, UUID playerUUID) {

    public JSONObject toJSON() {
        return new JSONObject(String.format(
                "{\"width\":%f,\"height\":%f,\"max_framerate\":%f,\"player_uuid\":%s}",
                width, height, maxFramerate, playerUUID.toString()
        ));
    }

}
