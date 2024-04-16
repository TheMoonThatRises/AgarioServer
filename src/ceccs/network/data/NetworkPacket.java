package ceccs.network.data;

import ceccs.network.OP_CODES;
import org.json.JSONObject;

public record NetworkPacket(int op, JSONObject data) {

    public NetworkPacket(int op, JSONObject data) {
        this.op = op;
        this.data = data;
    }

    public NetworkPacket(int op, String data) {
        this(op, new JSONObject(data));
    }

    public NetworkPacket(OP_CODES op, JSONObject data) {
        this(op.getValue(), data);
    }

    public NetworkPacket(OP_CODES op, String data) {
        this(op, new JSONObject(data));
    }

    public static NetworkPacket fromString(String packet) {
        JSONObject object = new JSONObject(packet);

        return new NetworkPacket(object.getInt("op"), object.getJSONObject("data"));
    }

    public JSONObject toJSON() {
        return new JSONObject(String.format("{\"op\":%d,\"data\":%s}", op, data.toString()));
    }

}
