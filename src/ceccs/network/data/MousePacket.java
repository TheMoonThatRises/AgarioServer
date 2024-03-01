package ceccs.network.data;

import org.json.JSONObject;

public class MousePacket {

    final public double x;
    final public double y;

    public MousePacket(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public static MousePacket fromJSON(JSONObject packet) {
        return new MousePacket(
            packet.getDouble("x"),
            packet.getDouble("y")
        );
    }

}
