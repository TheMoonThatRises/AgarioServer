package ceccs.network.data;

import org.json.JSONObject;

public record MousePacket(double x, double y) {

    public static MousePacket fromJSON(JSONObject packet) {
        return new MousePacket(
                packet.getDouble("x"),
                packet.getDouble("y")
        );
    }

}
