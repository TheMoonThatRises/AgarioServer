package ceccs.network.data;

import org.json.JSONObject;

public record IdentifyPacket(String username, double screenWidth, double screenHeight) {

    public static IdentifyPacket fromJSON(JSONObject packet) {
        return new IdentifyPacket(
                packet.getString("username"),
                packet.getDouble("screen_width"),
                packet.getDouble("screen_height")
        );
    }

}
