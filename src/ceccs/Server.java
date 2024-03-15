package ceccs;

import ceccs.game.Game;
import ceccs.network.NetworkHandler;
import ceccs.utils.Configurations;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Scanner;

public class Server {

    final public static double maxFramerate = 120;

    final public static Configurations configs = Configurations.shared;

    public static void main(String[] args) throws IOException {
        InetSocketAddress server = getServer();

        configs.setProperty("server.ip", server.getHostString());
        configs.setProperty("server.port", String.valueOf(server.getPort()));

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

        System.out.print("load previous server config? ([y]/n): ");

        if (!scanner.nextLine().toLowerCase().contains("n") ) {
            serverIp = configs.getProperty("server.ip");
            serverPort = Integer.parseInt(configs.getProperty("server.port"));

            System.out.println();

            return new InetSocketAddress(serverIp, serverPort);
        }

        while (serverIp.isEmpty()) {
            System.out.print("\nenter server ip: ");

            serverIp = scanner.nextLine().trim();
        }

        while (serverPort == null) {
            System.out.print("\nenter server port: ");

            try {
                serverPort = Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException exception) {
                exception.printStackTrace();

                System.err.println("failed to parse server port input");
            }
        }

        System.out.println();

        return new InetSocketAddress(serverIp, serverPort);
    }

}
