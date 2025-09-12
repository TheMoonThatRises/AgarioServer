package ceccs;

import ceccs.game.Game;
import ceccs.network.NetworkHandler;
import ceccs.network.utils.AddressCompress;
import ceccs.utils.Configurations;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Scanner;

public class Server {

    final public static double maxFramerate = 100;

    final public static Configurations configs = Configurations.shared;

    public static void main(String[] args) throws IOException {
        queryConfigs();

        InetSocketAddress server = new InetSocketAddress(
                configs.getProperty("server.ip"),
                Integer.parseInt(configs.getProperty("server.port"))
        );

        System.out.println("spinning up game env");
        Game game = new Game();

        game.loadEnvironment();

        System.out.println("loading network handler");
        System.out.println("using address " + server.getAddress().getHostAddress() + ":" + server.getPort());
        NetworkHandler networkHandler = new NetworkHandler(server, game);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            networkHandler.stop();
            networkHandler.terminateAll();
        }));

        System.out.println("starting network handler");
        networkHandler.start();

        System.out.println(
                "server code: " +
                        AddressCompress.encodeAddress(
                                server.getAddress().getHostAddress(),
                                String.valueOf(server.getPort())
                        )
        );

        game.startHeartbeat();
    }

    private static void queryConfigs() {
        String serverIp = "";
        Integer serverPort = null;

        Scanner scanner = new Scanner(System.in);

        if (
                !configs.getProperty("server.ip").isEmpty() &&
                        !configs.getProperty("server.port").isEmpty()
        ) {
            System.out.print("load previous server config? ([y]/n): ");

            if (!scanner.nextLine().toLowerCase().contains("n")) {
                System.out.println();

                return;
            }
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

        configs.setProperty("server.ip", serverIp);
        configs.setProperty("server.port", String.valueOf(serverPort));
    }

}
