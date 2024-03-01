package ceccs;

import ceccs.game.Game;
import ceccs.network.NetworkHandler;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class Server {

    final public static int PORT = 2351;
    final public static InetAddress ADDRESS;

    static {
        InetAddress tmpAddress;
        try {
            tmpAddress = InetAddress.getLocalHost();
        } catch (UnknownHostException exception) {
            System.err.println("failed to get localhost address: " + exception);
            tmpAddress = null;
        }
        ADDRESS = tmpAddress;
    }

    public static void main(String[] args) throws IOException {
        System.out.println("spinning up game env");
        Game game = new Game();

        game.loadEnvironment();

        System.out.println("loading network handler");
        System.out.println("using address " + ADDRESS + ":" + PORT);
        NetworkHandler networkHandler = new NetworkHandler(PORT, ADDRESS, game);

        System.out.println("starting network handler");
        networkHandler.start();

        game.startHeartbeat();
    }

}
