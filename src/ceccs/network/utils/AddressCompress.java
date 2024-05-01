package ceccs.network.utils;

import javafx.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

public class AddressCompress {

    final private static ArrayList<Character> map = new ArrayList<>(Arrays.asList(
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
            'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
            'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
    ));
    final private static int mapSize = map.size();

    private static String padString(String string, String padValue, int size) {
        if (string.length() < size) {
            string = padValue.repeat(size - string.length()) + string;
        }

        return string;
    }

    private static String encodeLong(long address) {
        StringBuilder encoded = new StringBuilder();

        while (address > 0) {
            int value = (int) (address % mapSize);
            address /= mapSize;
            encoded.append(map.get(value));
        }

        return encoded.toString();
    }

    private static long decodeLong(String address) {
        long decoded = 0;

        for (int i = address.length() - 1; i >= 0; i--) {
            int value = map.indexOf(address.charAt(i));
            decoded = decoded * mapSize + value;
        }

        return decoded;
    }

    public static String encodeAddress(String address, String port) {
        address = Arrays.stream(address.split("\\."))
                .map(value -> padString(value, "0", 3))
                .collect(Collectors.joining());

        address += padString(port, "0", 5);

        long addressNum = Long.parseLong(address);

        return encodeLong(addressNum);
    }

    public static Pair<String, Integer> decodeAddress(String encoded) {
        String addressNum = String.valueOf(decodeLong(encoded));

        int port = Integer.parseInt(addressNum.substring(addressNum.length() - 5));

        addressNum = addressNum.substring(0, addressNum.length() - 5);

        String[] ipParts = new String[4];

        for (int i = addressNum.length(); i > 0; i -= 3) {
            ipParts[(i / 3) - 1] = String.valueOf(
                    Integer.parseInt(
                            addressNum.substring(Math.max(i - 3, 0), i)
                    )
            );
        }

        String ip = String.join(".", ipParts);

        return new Pair<>(ip, port);
    }

}
