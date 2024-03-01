package ceccs.network.data;

import org.json.JSONObject;

import java.util.UUID;

public class RegisterPacket {

    final public double width;
    final public double height;

    final public UUID playerUUID;

    public RegisterPacket(double width, double height, UUID playerUUID) {
        this.width = width;
        this.height = height;

        this.playerUUID = playerUUID;
    }

    public JSONObject toJSON() {
        return new JSONObject(String.format(
            "{\"width\":%f,\"height\":%f,\"player_uuid\":%s}",
            width, height, playerUUID.toString()
        ));
    }

}
