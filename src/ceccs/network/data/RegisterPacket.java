package ceccs.network.data;

import org.json.JSONObject;

import java.util.UUID;

public record RegisterPacket(double width, double height, UUID playerUUID) {

    public JSONObject toJSON() {
        return new JSONObject(String.format(
                "{\"width\":%f,\"height\":%f,\"player_uuid\":%s}",
                width, height, playerUUID.toString()
        ));
    }

}
