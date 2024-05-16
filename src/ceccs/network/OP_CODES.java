package ceccs.network;

import java.util.Arrays;

public enum OP_CODES {

    CLIENT_IDENTIFY(0),
    CLIENT_PING(1),
    CLIENT_MOUSE_UPDATE(4),
    CLIENT_KEYBOARD_UPDATE(5),
    CLIENT_TERMINATE(9),

    SERVER_IDENTIFY_OK(10),
    SERVER_PONG(11),
    SERVER_GAME_STATE(14),
    SERVER_GAME_STATE_GZIP(15),
    SERVER_TERMINATE(19),

    OP_CODE_ERROR(100),
    CLIENT_UNIDENTIFIED_ERROR(101),

    GZIP_PACKET(1001);

    final private int opcode;

    OP_CODES(int opcode) {
        this.opcode = opcode;
    }

    public static OP_CODES fromValue(int value) {
        return Arrays.stream(values())
                .filter(opcode -> opcode.opcode == value)
                .findFirst()
                .orElse(OP_CODE_ERROR);
    }

    public int getValue() {
        return opcode;
    }

}
