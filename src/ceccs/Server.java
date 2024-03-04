package ceccs;

import ceccs.game.Game;
import ceccs.network.NetworkHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Scanner;

public class Server {

    final public static double maxFramerate = 120;

//    final public static int PORT = 2351;
//    final public static InetAddress ADDRESS;
//
//    static {
//        InetAddress tmpAddress;
//        try {
//            tmpAddress = InetAddress.getLocalHost();
//        } catch (UnknownHostException exception) {
//            System.err.println("failed to get localhost address: " + exception);
//            tmpAddress = null;
//        }
//        ADDRESS = tmpAddress;
//    }

    public static void main(String[] args) throws IOException {
        InetSocketAddress server = getServer();

        System.out.println("spinning up game env");
        Game game = new Game();

        game.loadEnvironment();

        System.out.println("loading network handler");
        System.out.println("using address " + server.getAddress() + ":" + server.getPort());
        NetworkHandler networkHandler = new NetworkHandler(server, game);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            networkHandler.stop();
            networkHandler.terminateAll();
        }));

        System.out.println("starting network handler");
        networkHandler.start();

        game.startHeartbeat();
    }

    private static InetSocketAddress getServer() {
        String serverIp = "";
        Integer serverPort = null;

        Scanner scanner = new Scanner(System.in);

        while (serverIp.isEmpty()) {
            System.out.print("\nenter server ip: ");

            serverIp = scanner.nextLine().trim();
        }

        while (serverPort == null) {
            System.out.print("\nenter server port: ");

            try {
                serverPort = Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException exception) {
                System.err.println("failed to parse server port input: " + exception);
            }
        }

        System.out.println();

        return new InetSocketAddress(serverIp, serverPort);
    }

}
