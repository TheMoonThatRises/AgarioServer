package ceccs.network.data;

import org.json.JSONObject;

public record KeyPacket(int keycode, boolean pressed) {

    public static KeyPacket fromJSON(JSONObject packet) {
        return new KeyPacket(packet.getInt("keycode"), packet.getBoolean("pressed"));
    }

}
