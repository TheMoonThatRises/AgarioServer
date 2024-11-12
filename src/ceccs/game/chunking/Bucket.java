package ceccs.game.chunking;

import ceccs.game.Game;
import ceccs.game.objects.BLOB_TYPES;
import ceccs.game.objects.Camera;
import ceccs.game.objects.elements.Blob;
import ceccs.game.objects.elements.Player;
import ceccs.game.utils.PhysicsMap;
import ceccs.network.utils.CustomID;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Bucket {

    final private ConcurrentHashMap<String, Chunk> chunks;
    final private Set<String> activeChunks;
    final private ConcurrentHashMap<String, Long> unloadQueue;
    final private ConcurrentHashMap<String, Long> unloadCooldown;

    final private Game game;

    public Bucket(Game game) {
        this.chunks = new ConcurrentHashMap<>();
        this.activeChunks = ConcurrentHashMap.newKeySet();

        this.unloadQueue = new ConcurrentHashMap<>();
        this.unloadCooldown = new ConcurrentHashMap<>();

        this.game = game;

        for (int xloc = 0; xloc < PhysicsMap.width / PhysicsMap.chunkWidth; ++xloc) {
            for (int yloc = 0; yloc < PhysicsMap.height / PhysicsMap.chunkHeight; ++yloc) {
                String locId = locToId(xloc, yloc);

                this.chunks.put(
                        locId,
                        new Chunk(locId, game)
                );
            }
        }
    }

    private String locToId(int x, int y) {
        return String.format("%d,%d", x, y);
    }

    private String getChunkLocId(double x, double y) {
        int xLoc = (int) Math.floor(x / PhysicsMap.chunkWidth);
        int yLoc = (int) Math.floor(y / PhysicsMap.chunkHeight);

        String locId = locToId(xLoc, yLoc);

        if (!this.chunks.containsKey(locId)) {
            System.err.printf("locId %s does not exist, defaulting to 0,0\n", locId);

            return "0,0";
        }

        return locId;
    }

    private int[] getCoordFromLocId(String locId) {
        String[] locSplit = locId.split(",");

        int xLoc = Integer.parseInt(locSplit[0]);
        int yLoc = Integer.parseInt(locSplit[1]);

        return new int[]{xLoc, yLoc};
    }

    private String getDirChunkLocId(String currLocId, double vx, double vy) {
        int[] loc = getCoordFromLocId(currLocId);

        loc[0] += vx >= 0 ? 1 : -1;
        loc[1] += vy >= 0 ? 1 : -1;

        String locId = locToId(loc[0], loc[1]);

        return chunks.containsKey(locId) ? locId : currLocId;
    }

    public Chunk updateChunkManagedItem(Blob blob) {
        String locId = getChunkLocId(blob.getX(), blob.getY());

        CustomID blobId = blob.uuid;

        if (blob.getType() == BLOB_TYPES.PLAYER) {
            activeChunks.add(locId);

            Player player = game.findParentPlayer(blob.uuid);

            if (player == null) {
                System.err.printf("bucket update: unable to retrieve parent player from child blob id \"%s\"\n", blob.uuid);
            } else {
                blobId = player.uuid;
            }
        }

        if (blob.getVx() != 0 || blob.getVy() != 0) {
            activeChunks.add(locId);
            activeChunks.add(getDirChunkLocId(locId, blob.getVx(), blob.getVy()));
        }

        if (blob.getParentChunk() != null) {
            if (locId.equals(blob.getParentChunk().locId)) {
                return chunks.get(locId);
            } else {
                chunks.get(blob.getParentChunk().locId).removeManagedItem(blobId);
            }
        }

        CustomID finalBlobId = blobId;

        if (!(blob.getType() == BLOB_TYPES.PLAYER && blobId == blob.uuid && activeChunks.stream().noneMatch(id -> chunks.get(id).hasManagedItem(finalBlobId)))) {
            chunks.get(locId).addManagedItem(blobId, blob.getType());
        }

        return chunks.get(locId);
    }

    public ArrayList<Chunk> getVisibleChunks(Camera camera) {
        return new ArrayList<>(
                activeChunks.stream().filter(id -> {
                            int[] loc = getCoordFromLocId(id);

                            double relX = (loc[0] * PhysicsMap.chunkWidth - camera.getX()) * camera.getScale();
                            double relY = (loc[1] * PhysicsMap.chunkHeight - camera.getY()) * camera.getScale();

                            return !(
                                    relX + PhysicsMap.chunkWidth < -10 ||
                                            relX - PhysicsMap.chunkWidth > camera.getScreenWidth() + 10 ||
                                            relY + PhysicsMap.chunkHeight < -10 ||
                                            relY - PhysicsMap.chunkHeight > camera.getScreenHeight() + 10
                            );
                        })
                        .map(chunks::get)
                        .toList()
        );
    }

    public void playerAdded(Player player) {
        String locId = getChunkLocId(player.getX(), player.getY());

        chunks.get(locId).addManagedItem(player.uuid, BLOB_TYPES.PLAYER);

        activeChunks.add(locId);
    }

    public void playerRemoved(CustomID playerId) {
        for (String locId : activeChunks) {
            chunks.get(locId).removeManagedItem(playerId);
        }
    }

    public void tickBucket(long time) {
        for (String locId : activeChunks) {
            chunks.get(locId).tickChunkPosition();
        }

        for (String locId : activeChunks) {
            Chunk chunk = chunks.get(locId);

            chunk.tickChunkCollision(time);

            if (unloadQueue.containsKey(locId)) {
                if (time - unloadQueue.get(locId) > 3_000_000_000L) {
                    if (!chunk.hasActiveBlobs()) {
                        activeChunks.remove(locId);
                        unloadCooldown.remove(locId);
                    } else {
                        unloadCooldown.put(locId, time);
                    }

                    unloadQueue.remove(locId);
                }
            } else if (!unloadCooldown.containsKey(locId) || time - unloadCooldown.get(locId) > 3_000_000_000L) {
                unloadQueue.put(locId, time);
            }
        }
    }

}
