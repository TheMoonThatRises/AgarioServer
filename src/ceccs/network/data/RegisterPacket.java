package ceccs.network.data;

import ceccs.network.utils.CustomID;
import org.json.JSONObject;

public record RegisterPacket(double width, double height, double maxFramerate, CustomID playerUUID) {

    public JSONObject toJSON() {
        return new JSONObject()
                .put("width", width)
                .put("height", height)
                .put("max_framerate", maxFramerate)
                .put("player_uuid", playerUUID.toString());
    }

}
