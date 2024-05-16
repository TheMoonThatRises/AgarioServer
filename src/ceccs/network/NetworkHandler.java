package ceccs.network;

import ceccs.Server;
import ceccs.game.Game;
import ceccs.game.utils.PhysicsMap;
import ceccs.network.data.*;
import ceccs.network.utils.CustomID;
import ceccs.network.utils.GZip;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

public class NetworkHandler {

    final private static int maxPacketSize = 65_534;

    final private static long timeout = 5_000_000_000L;

    final public ConcurrentHashMap<CustomID, PlayerSocket> playerSockets;
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
                byte[] buf = new byte[maxPacketSize];
                DatagramPacket inPacket = new DatagramPacket(buf, buf.length);

                try {
                    serverSocket.receive(inPacket);

                    new Thread(() -> handleIncomingPacket(inPacket)).start();
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
                        game.despawnPlayer(playerSocket.getID());
                        playerSockets.remove(playerSocket.getID());

                        if (didTimeout) {
                            System.out.printf(
                                    "player with uuid %s and address %s:%d timed out\n",
                                    playerSocket.getID(), playerSocket.getAddress(), playerSocket.getPort()
                            );
                        }

                        handleWritePacket(playerSocket, OP_CODES.SERVER_TERMINATE);

                        continue;
                    }

                    if (game.players.containsKey(playerSocket.getID())) {
                        JSONObject gameData = game.getGameState(playerSocket.getID());
                        new Thread(() -> handleWritePacket(playerSocket, OP_CODES.SERVER_GAME_STATE, gameData)).start();
                    }
                }
            }
        };

        this.socketWriterTimer = new Timer("socket_writer_timer");
    }

    private void handleIncomingPacket(DatagramPacket packet) {
        InetAddress incomingAddress = packet.getAddress();
        int port = packet.getPort();

        CustomID playerUUID = PlayerSocket.toID(incomingAddress, port);

        try {
            String received = new String(packet.getData());
            NetworkPacket networkPacket = NetworkPacket.fromString(received);

            OP_CODES opcode = networkPacket.op();
            JSONObject packetData = networkPacket.data();

            if (opcode == OP_CODES.GZIP_PACKET) {
                try {
                    GZipPacket gZipPacket = GZipPacket.fromJSON(packetData);

                    opcode = gZipPacket.op();
                    packetData = new JSONObject(new String(GZip.decompress(gZipPacket.data())));
                } catch (IOException exception) {
                    exception.printStackTrace();

                    System.err.println("failed to decompress gzip packet: " + exception);

                    return;
                }
            }

            if (opcode != OP_CODES.CLIENT_IDENTIFY && !playerSockets.containsKey(playerUUID)) {
                handleWritePacket(new PlayerSocket(incomingAddress, port), OP_CODES.CLIENT_UNIDENTIFIED_ERROR);

                return;
            }

            switch (opcode) {
                case CLIENT_IDENTIFY -> {
                    PlayerSocket playerSocket = new PlayerSocket(incomingAddress, port);
                    playerSockets.put(playerUUID, playerSocket);
                    game.spawnPlayer(playerSocket, IdentifyPacket.fromJSON(packetData));

                    System.out.printf(
                            "player with uuid %s and address %s:%d requested to connect\n",
                            playerUUID, incomingAddress, port
                    );

                    handleWritePacket(
                            playerSockets.get(playerUUID),
                            OP_CODES.SERVER_IDENTIFY_OK,
                            new RegisterPacket(
                                    PhysicsMap.width,
                                    PhysicsMap.height,
                                    Server.maxFramerate,
                                    playerUUID
                            ).toJSON()
                    );
                }
                case CLIENT_PING -> {
                    playerSockets.get(playerUUID).updateLastPing();

                    handleWritePacket(
                            playerSockets.get(playerUUID),
                            OP_CODES.SERVER_PONG,
                            new JSONObject()
                                    .put("tps", game.getTps())
                                    .put("leaderboard", game.getLeaderboard(playerUUID))
                    );
                }
                case CLIENT_MOUSE_UPDATE -> game.updatePlayerMouse(playerUUID, MousePacket.fromJSON(packetData));
                case CLIENT_KEYBOARD_UPDATE -> game.updatePlayerKey(playerUUID, KeyPacket.fromJSON(packetData));
                case CLIENT_TERMINATE -> {
                    playerSockets.get(playerUUID).setTerminate();

                    System.out.printf(
                            "player with uuid %s and address %s:%d requested to terminate\n",
                            playerUUID, incomingAddress, port
                    );
                }
                case OP_CODE_ERROR -> {
                    PlayerSocket playerSocket = playerSockets.containsKey(playerUUID)
                            ? playerSockets.get(playerUUID)
                            : new PlayerSocket(incomingAddress, port);

                    handleWritePacket(playerSocket, OP_CODES.OP_CODE_ERROR);
                }
                default -> System.out.println("unhandled op code: " + opcode);
            }
        } catch (JSONException exception) {
            exception.printStackTrace();

            System.err.println("failed to parse json: " + exception);
        }
    }

    private void handleWritePacket(PlayerSocket playerSocket, OP_CODES op, JSONObject data) {
        NetworkPacket networkPacket = new NetworkPacket(op, data);

        byte[] byteData = networkPacket.toJSON().toString().getBytes();

        if (byteData.length > maxPacketSize - 100) {
            try {
                GZipPacket gZipPacket = new GZipPacket(op, data);

                networkPacket = new NetworkPacket(OP_CODES.GZIP_PACKET, gZipPacket.toJSON());

                byteData = networkPacket.toJSON().toString().getBytes();
            } catch (IOException exception) {
                exception.printStackTrace();

                System.err.println("failed to compress packet: " + exception);

                return;
            }
        }

        DatagramPacket packet = new DatagramPacket(byteData, byteData.length, playerSocket.getAddress(), playerSocket.getPort());

        try {
            serverSocket.send(packet);
        } catch (IOException exception) {
            exception.printStackTrace();

            System.err.println("failed to send game packet: len " + byteData.length);
        }
    }

    private void handleWritePacket(PlayerSocket playerSocket, OP_CODES op) {
        handleWritePacket(playerSocket, op, new JSONObject("{}"));
    }

    public void start() {
        socketListener.start();
        socketWriterTimer.scheduleAtFixedRate(socketWriter, 0, 10);
    }

    public void stop() {
        socketListener.interrupt();
        socketWriterTimer.cancel();
    }

    public void terminateAll() {
        playerSockets.values().forEach(player -> handleWritePacket(player, OP_CODES.SERVER_TERMINATE));
    }

}
