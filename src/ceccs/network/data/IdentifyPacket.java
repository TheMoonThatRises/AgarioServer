package ceccs.network.data;

import org.json.JSONObject;

final public class IdentifyPacket {

    final public double screenWidth;
    final public double screenHeight;

    public IdentifyPacket(double screenWidth, double screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }

    public static IdentifyPacket fromJSON(JSONObject packet) {
        return new IdentifyPacket(
            packet.getDouble("screen_width"),
            packet.getDouble("screen_height")
        );
    }

}
