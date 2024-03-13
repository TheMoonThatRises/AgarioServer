package ceccs.network;

import ceccs.Server;
import ceccs.game.Game;
import ceccs.game.utils.PhysicsMap;
import ceccs.network.data.*;
import ceccs.network.utils.GZip;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class NetworkHandler {

    final private static long timeout = 10_000_000_000L;

    final public ConcurrentHashMap<UUID, PlayerSocket> playerSockets;
    final private DatagramSocket serverSocket;

    final private Thread socketListener;
    final private TimerTask socketWriter;

    final private Timer socketWriterTimer;

    final private Game game;

    public NetworkHandler(InetSocketAddress server, Game game) throws IOException {
        this.playerSockets = new ConcurrentHashMap<>();
        this.serverSocket = new DatagramSocket(server.getPort(), server.getAddress());

        this.serverSocket.setTrafficClass(0x10);

        this.game = game;

        this.socketListener = new Thread(() -> {
            while (true) {
                byte[] buf = new byte[65534];
                DatagramPacket inPacket = new DatagramPacket(buf, buf.length);

                try {
                    serverSocket.receive(inPacket);

                    handleIncomingPacket(inPacket);
                } catch (IOException exception) {
                    exception.printStackTrace();

                    System.err.println("failed receiving incoming packet");
                }
            }
        });
        this.socketWriter = new TimerTask() {
            @Override
            public void run() {
                ArrayList<PlayerSocket> socketList = new ArrayList<>(playerSockets.values());

                for (int i = socketList.size() - 1; i >= 0; --i) {
                    PlayerSocket playerSocket = socketList.get(i);

                    boolean didTimeout = playerSocket.getLastPing() + timeout < System.nanoTime();

                    if (
                        playerSocket.isShouldTerminate() ||
                        didTimeout
                    ) {
                        game.despawnPlayer(playerSocket.getUUID());
                        playerSockets.remove(playerSocket.getUUID());

                        if (didTimeout) {
                            System.out.printf(
                                "player with uuid %s and address %s:%d timed out\n",
                                playerSocket.getUUID(), playerSocket.getAddress(), playerSocket.getPort()
                            );
                        }

                        handleWritePacket(playerSocket, OP_CODES.SERVER_TERMINATE);

                        continue;
                    }

                    if (game.players.containsKey(playerSocket.getUUID())) {
                        JSONObject gameData = game.getGameState(playerSocket.getUUID());
                        handleWritePacket(playerSocket, OP_CODES.SERVER_GAME_STATE, gameData);
                    }
                }
            }
        };

        this.socketWriterTimer = new Timer("socket_writer_timer");
    }

    private void handleIncomingPacket(DatagramPacket packet) {
        InetAddress incomingAddress = packet.getAddress();
        int port = packet.getPort();

        UUID playerUUID = PlayerSocket.toUUID(incomingAddress, port);

        try {
            String received = new String(GZip.decompress(packet.getData()));
            NetworkPacket networkPacket = NetworkPacket.fromString(received);

            Optional<OP_CODES> opcode = OP_CODES.fromValue(networkPacket.op);

            opcode.ifPresentOrElse(op -> {
                if (op != OP_CODES.CLIENT_IDENTIFY && !playerSockets.containsKey(playerUUID)) {
                    handleWritePacket(new PlayerSocket(incomingAddress, port), OP_CODES.CLIENT_UNIDENTIFIED_ERROR);

                    return;
                }

                switch (op) {
                    case CLIENT_IDENTIFY -> {
                        PlayerSocket playerSocket = new PlayerSocket(incomingAddress, port);
                        playerSockets.put(playerUUID, playerSocket);
                        game.spawnPlayer(playerSocket, IdentifyPacket.fromJSON(networkPacket.data));

                        System.out.printf(
                            "player with uuid %s and address %s:%d requested to connect\n",
                            playerUUID, incomingAddress, port
                        );

                        handleWritePacket(
                            playerSockets.get(playerUUID),
                            OP_CODES.SERVER_IDENTIFY_OK,
                            new RegisterPacket(PhysicsMap.width, PhysicsMap.height, Server.maxFramerate, playerUUID).toJSON()
                        );
                    }
                    case CLIENT_PING -> {
                        playerSockets.get(playerUUID).updateLastPing();

                        handleWritePacket(
                            playerSockets.get(playerUUID),
                            OP_CODES.SERVER_PONG
                        );
                    }
                    case CLIENT_MOUSE_UPDATE -> game.updatePlayerMouse(playerUUID, MousePacket.fromJSON(networkPacket.data));
                    case CLIENT_KEYBOARD_UPDATE -> game.updatePlayerKey(playerUUID, KeyPacket.fromJSON(networkPacket.data));
                    case CLIENT_TERMINATE -> {
                        playerSockets.get(playerUUID).setTerminate();

                        System.out.printf(
                            "player with uuid %s and address %s:%d requested to terminate\n",
                            playerUUID, incomingAddress, port
                        );
                    }
                    default -> System.out.println("unhandled op code: " + op);
                }
            }, () -> {
                PlayerSocket playerSocket = playerSockets.containsKey(playerUUID)
                        ? playerSockets.get(playerUUID)
                        : new PlayerSocket(incomingAddress, port);

                handleWritePacket(playerSocket, OP_CODES.OP_CODE_ERROR);
            });
        } catch (IOException exception) {
            exception.printStackTrace();

            System.err.println("failed to decompress packet: " + exception);
        }
    }

    private void handleWritePacket(PlayerSocket playerSocket, OP_CODES op, JSONObject data) {
        NetworkPacket networkPacket = new NetworkPacket(op, data);

        try {
            byte[] byteData = GZip.compress(networkPacket.toJSON().toString().getBytes());

            DatagramPacket packet = new DatagramPacket(byteData, byteData.length, playerSocket.getAddress(), playerSocket.getPort());

            try {
                serverSocket.send(packet);
            } catch (IOException exception) {
                exception.printStackTrace();

                System.err.println("failed to send game packet");
            }
        } catch (IOException exception) {
            exception.printStackTrace();

            System.err.println("failed to compress packet");
        }
    }

    private void handleWritePacket(PlayerSocket playerSocket, OP_CODES op) {
        handleWritePacket(playerSocket, op, new JSONObject("{}"));
    }

    public void start() {
        socketListener.start();
        socketWriterTimer.scheduleAtFixedRate(socketWriter, 0, 5);
    }

    public void stop() {
        socketListener.interrupt();
        socketWriterTimer.cancel();
    }

    public void terminateAll() {
        playerSockets.values().forEach(player -> handleWritePacket(player, OP_CODES.SERVER_TERMINATE));
    }

}
