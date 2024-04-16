package ceccs.network.utils;

import ceccs.game.utils.Utilities;

import java.io.Serializable;
import java.util.UUID;

public class CustomID implements Serializable, Comparable<CustomID> {

    final private String id;

    protected CustomID(String id) {
        this.id = id;
    }

    public static CustomID randomID() {
        return new CustomID(generateRandomID());
    }

    public String getID() {
        return id;
    }

    public String toString() {
        return id;
    }

    public static CustomID fromString(String id) {
        return new CustomID(id);
    }

    public static CustomID IDFromBytes(byte[] bytes) {
        String uuid = UUID.nameUUIDFromBytes(bytes).toString();

        StringBuilder str = new StringBuilder();

        for (int i = 0; i < 10; ++i) {
            str.append(uuid.charAt((i % 2 == 0 ? i : 10 - i) % 10));
        }

        return new CustomID(str.toString());
    }

    protected static String generateRandomID() {
        String symbols = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        StringBuilder str = new StringBuilder();

        for (int i = 0; i < 10; i++) {
            str.append(symbols.charAt(Utilities.random.nextInt(symbols.length())));
        }

        return str.toString();
    }

    @Override
    public int compareTo(CustomID o) {
        return o.toString().compareTo(id);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof CustomID) {
            return ((CustomID) o).id.equals(id);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
