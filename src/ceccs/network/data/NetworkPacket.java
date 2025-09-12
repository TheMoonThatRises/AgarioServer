package ceccs.network.data;

import ceccs.network.OP_CODES;
import org.json.JSONObject;

public record NetworkPacket(OP_CODES op, JSONObject data) {

    public static NetworkPacket fromString(String packet) {
        JSONObject object = new JSONObject(packet);

        OP_CODES op = OP_CODES.fromValue(object.getInt("op"));

        return new NetworkPacket(op, object.getJSONObject("data"));
    }

    public JSONObject toJSON() {
        return new JSONObject()
                .put("op", op.getValue())
                .put("data", data);
    }

}
