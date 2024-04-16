package ceccs.network;

import ceccs.network.utils.CustomID;

import java.net.InetAddress;

public class PlayerSocket {

    final private InetAddress address;
    final private int port;

    final private CustomID uuid;

    private boolean shouldTerminate;

    private long lastPing;

    public PlayerSocket(InetAddress address, int port) {
        this.address = address;
        this.port = port;
        this.shouldTerminate = false;

        this.uuid = PlayerSocket.toID(address, port);

        this.lastPing = System.nanoTime();
    }

    public static CustomID toID(InetAddress address, int port) {
        return CustomID.IDFromBytes((address.getHostAddress() + ":" + port).getBytes());
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public CustomID getID() {
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
