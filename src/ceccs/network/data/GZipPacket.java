package ceccs.network.data;

import ceccs.network.OP_CODES;
import ceccs.network.utils.GZip;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Base64;

public record GZipPacket(OP_CODES op, byte[] data) {

    public GZipPacket(OP_CODES op, JSONObject data) throws IOException {
        this(op, GZip.compress(data.toString().getBytes()));
    }

    public static GZipPacket fromJSON(JSONObject packet) {
        return new GZipPacket(
                OP_CODES.fromValue(packet.getInt("op")),
                Base64.getDecoder().decode(packet.getString("data"))
        );
    }

    public JSONObject toJSON() {
        return new JSONObject()
                .put("op", op.getValue())
                .put("data", Base64.getEncoder().encodeToString(data));
    }

}
