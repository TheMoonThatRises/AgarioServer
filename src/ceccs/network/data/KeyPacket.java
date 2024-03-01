package ceccs.network.data;

import org.json.JSONObject;

public class KeyPacket {

    final public int keycode;
    final public boolean pressed;

    public KeyPacket(int keycode, boolean pressed) {
        this.keycode = keycode;
        this.pressed = pressed;
    }

    public static KeyPacket fromJSON(JSONObject packet) {
        return new KeyPacket(packet.getInt("keycode"), packet.getBoolean("pressed"));
    }

}
