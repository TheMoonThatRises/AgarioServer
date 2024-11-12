package ceccs.game.chunking;

import ceccs.game.Game;
import ceccs.game.objects.BLOB_TYPES;
import ceccs.game.objects.elements.Blob;
import ceccs.game.objects.elements.Player;
import ceccs.network.utils.CustomID;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class Chunk {

    final public String locId;

    final private ConcurrentHashMap<CustomID, BLOB_TYPES> managedItems;

    final private Game game;

    public Chunk(String locId, Game game) {
        this.locId = locId;

        this.managedItems = new ConcurrentHashMap<>();
        this.game = game;
    }

    public void addManagedItem(CustomID id, BLOB_TYPES type) {
        managedItems.put(id, type);
    }

    public void removeManagedItem(CustomID id) {
        managedItems.remove(id);
    }

    private Blob getBlob(CustomID id, BLOB_TYPES type) {
        return switch (type) {
            case FOOD -> game.foods.get(id);
            case SPIKE -> game.viruses.get(id);
            case PELLET -> game.pellets.get(id);
            default -> null;
        };
    }

    private Player getPlayer(CustomID id) {
        return game.players.get(id);
    }

    public ArrayList<Blob> getAllBlobs() {
        return new ArrayList<>(
                managedItems.entrySet()
                .stream()
                .filter(set -> set.getValue() != BLOB_TYPES.PLAYER)
                .map(set -> getBlob(set.getKey(), set.getValue()))
                .toList()
        );
    }

    public void tickChunkPosition() {
        managedItems.entrySet().stream().parallel().forEach(set -> {
            BLOB_TYPES type = set.getValue();
            CustomID id = set.getKey();

            if (type == BLOB_TYPES.PLAYER) {
                Player player = getPlayer(id);

                if (player == null) {
                    System.err.printf("position tick: unable to retrieve player with id \"%s\"\n", id);
                } else {
                    player.positionTick();
                }
            } else {
                Blob blob = getBlob(id, type);

                if (blob == null) {
                    System.err.printf("position tick: unable to retrieve blob with id \"%s\" and type \"%s\"\n", id, type);
                } else {
                    getBlob(id, type).positionTick();
                }
            }
        });
    }

    public void tickChunkCollision(long time) {
        managedItems.entrySet().stream().parallel().forEach(set -> {
            BLOB_TYPES type = set.getValue();
            CustomID id = set.getKey();

            if (type == BLOB_TYPES.PLAYER) {
                Player player = getPlayer(id);

                if (player == null) {
                    System.err.printf("collision tick: unable to retrieve player with id \"%s\"\n", id);
                } else {
                    player.collisionTick(time);
                }
            } else {
                Blob blob = getBlob(id, type);

                if (blob == null) {
                    System.err.printf("collision tick: unable to retrieve blob with id \"%s\" and type \"%s\"\n", id, type);
                } else {
                    getBlob(id, type).collisionTick();
                }
            }
        });
    }

    public boolean hasActiveBlobs() {
        return managedItems.entrySet().stream().parallel().anyMatch(set -> {
            if (set.getValue() == BLOB_TYPES.PLAYER) {
                return true;
            } else {
                Blob blob = getBlob(set.getKey(), set.getValue());

                if (blob == null) {
                    System.err.printf("collision tick: unable to retrieve blob with id \"%s\" and type \"%s\"\n", set.getKey(), set.getValue());
                    return false;
                } else {
                    return blob.getVy() != 0 || blob.getVx() != 0;
                }
            }
        });
    }

}
