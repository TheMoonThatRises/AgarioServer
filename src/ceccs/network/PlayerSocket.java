package ceccs.network;

import java.net.InetAddress;
import java.util.UUID;

public class PlayerSocket {

    final private InetAddress address;
    final private int port;

    final private UUID uuid;

    private boolean shouldTerminate;

    private long lastPing;

    public PlayerSocket(InetAddress address, int port) {
        this.address = address;
        this.port = port;
        this.shouldTerminate = false;

        this.uuid = PlayerSocket.toUUID(address, port);

        this.lastPing = System.nanoTime();
    }

    public static UUID toUUID(InetAddress address, int port) {
        return UUID.nameUUIDFromBytes((address.getHostAddress() + ":" + port).getBytes());
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public UUID getUUID() {
        return uuid;
    }

    public void setTerminate() {
        shouldTerminate = true;
    }

    public boolean isShouldTerminate() {
        return shouldTerminate;
    }

    public void updateLastPing() {
        lastPing = System.nanoTime();
    }

    public long getLastPing() {
        return lastPing;
    }

}
