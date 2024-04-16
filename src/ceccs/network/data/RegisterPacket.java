package ceccs.network.data;

import ceccs.network.utils.CustomID;
import org.json.JSONObject;

public record RegisterPacket(double width, double height, double maxFramerate, CustomID playerUUID) {

    public JSONObject toJSON() {
        return new JSONObject(String.format(
                "{\"width\":%f,\"height\":%f,\"max_framerate\":%f,\"player_uuid\":\"%s\"}",
                width, height, maxFramerate, playerUUID.toString()
        ));
    }

}
