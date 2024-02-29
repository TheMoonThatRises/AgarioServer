package ceccs.network.data;

import org.json.JSONObject;

public record IdentifyPacket(double screenWidth, double screenHeight) {

    public static IdentifyPacket fromJSON(JSONObject packet) {
        return new IdentifyPacket(
                packet.getDouble("screen_width"),
                packet.getDouble("screen_height")
        );
    }

}
