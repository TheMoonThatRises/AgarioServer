package ceccs.game.objects.elements;

import ceccs.game.Game;
import ceccs.game.objects.BLOB_TYPES;
import ceccs.game.objects.Camera;
import ceccs.game.utils.ConsolidateBlobs;
import ceccs.game.utils.PhysicsMap;
import ceccs.game.utils.Utilities;
import ceccs.network.data.IdentifyPacket;
import ceccs.network.data.KeyPacket;
import ceccs.network.data.MousePacket;
import javafx.scene.paint.Paint;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static ceccs.game.configs.PelletConfigs.*;
import static ceccs.game.configs.PlayerConfigs.*;
import static ceccs.game.configs.VirusConfigs.virusConsumeMass;
import static ceccs.game.utils.Utilities.*;

public class Player {

    protected static class Cooldowns {
        public long pellet;
        public long split;
        public long merge;

        public Cooldowns() {
            this.pellet = 0;
            this.split = 0;
            this.merge = 0;
        }

        public JSONObject toJSON() {
            return new JSONObject(String.format(
                "{\"pellet\":%d,\"split\":%d,\"merge\":%d}",
                pellet, split, merge
            ));
        }
    }

    protected static class PlayerBlob extends Blob {

        final public UUID parentUUID;

        protected double maxVx;
        protected double maxVy;

        protected boolean hasSplitSpeedBoost;

        protected double splitBoostVelocity;

        protected Cooldowns cooldowns;

        protected long lastDecayTick;

        public PlayerBlob(double x, double y, double vx, double vy, double ax, double ay, double mass, boolean hasSplitSpeedBoost, Paint fill, UUID parentUUID, UUID uuid, Game game) {
            super(x, y, vx, vy, ax, ay, mass, fill, game, uuid, null);

            this.maxVx = 0;
            this.maxVy = 0;

            this.hasSplitSpeedBoost = hasSplitSpeedBoost;
            this.splitBoostVelocity = playerSplitVelocity;

            this.cooldowns = new Cooldowns();

            this.parentUUID = parentUUID;

            this.lastDecayTick = 0;
        }

        public PlayerBlob(double x, double y, double vx, double vy, double mass, Paint fill, UUID parentUUID, UUID uuid, Game game) {
            this(x, y, vx, vy, playerMouseAcc, playerMouseAcc, mass, false, fill, parentUUID, uuid, game);
        }

        public PlayerBlob(double x, double y, double mass, boolean hasSplitSpeedBoost, Paint fill, UUID parentUUID, UUID uuid, Game game) {
            this(x, y, 0, 0, playerMouseAcc, playerMouseAcc, mass, hasSplitSpeedBoost, fill, parentUUID, uuid, game);
        }

        @Override
        public BLOB_TYPES getType() {
            return BLOB_TYPES.PLAYER;
        }

        public void positionTick(Player player, MousePacket mouseEvent) {
            Double relX = null;
            Double relY = null;

            if (mouseEvent != null) {
                relX = mouseEvent.x() - getRelativeX(player);
                relY = mouseEvent.y() - getRelativeY(player);

                maxVx = playerVelocities[closestNumber(playerVelocities, relX / 1000)];
                maxVy = playerVelocities[closestNumber(playerVelocities, relY / 1000)];
            }

            double velScale = calcVelocityModifier(mass);

            ax = maxVx < 0
                ? -Math.abs(ax)
                : Math.abs(ax);
            ay = maxVy < 0
                ? -Math.abs(ay)
                : Math.abs(ay);

            vx = (
                Math.abs(vx) < Math.abs(maxVx)
                    ? vx + ax
                    : maxVx
            );
            vy = (
                Math.abs(vy) < Math.abs(maxVy)
                    ? vy + ay
                    : maxVy
            );

            if (hasSplitSpeedBoost && relX != null) {
                splitBoostVelocity -= playerSplitDecay;

                if (splitBoostVelocity <= 0) {
                    hasSplitSpeedBoost = false;
                } else {
                    double delta = Math.atan2(relY, relX);

                    double sVX = splitBoostVelocity * Math.cos(delta);
                    double sVY = splitBoostVelocity * Math.sin(delta);

                    vx += sVX;
                    vy += sVY;
                }
            }

            x += vx * velScale;
            y += vy * velScale;
        }

        public void tickDelay(long time) {
            if (lastDecayTick + 1_000_000_000 < time) {
                if (mass > playerMinMassDecay) {
                    mass *= 0.998;
                }

                lastDecayTick = time;
            }
        }

        @Override
        public JSONObject toJSON() {
            JSONObject parent = super.toJSON();

            parent.put("parent_uuid", parentUUID);
            parent.put("max_vx", maxVx);
            parent.put("max_vy", maxVy);
            parent.put("has_split_speed_boost", hasSplitSpeedBoost);
            parent.put("split_boost_velocity", splitBoostVelocity);

            parent.put("cooldowns", cooldowns.toJSON());

            return parent;
        }

        public double getRelativeX(Player player) {
            Camera camera = new Camera(player);

            return (x - camera.getX()) * camera.getScale();
        }

        public double getRelativeY(Player player) {
            Camera camera = new Camera(player);

            return (y - camera.getY()) * camera.getScale();
        }
    }

    final public UUID uuid;
    final public IdentifyPacket identifyPacket;

    protected ConcurrentHashMap<UUID, PlayerBlob> playerBlobs;

    MousePacket mouseEvent;
    ConcurrentHashMap<Integer, Boolean> keyEvents;

    final protected Game game;

    public Player(double x, double y, double vx, double vy, double mass, Paint fill, UUID uuid, IdentifyPacket identifyPacket, Game game) {
        this.playerBlobs = new ConcurrentHashMap<>();

        UUID childUUID = UUID.randomUUID();

        this.playerBlobs.put(childUUID, new PlayerBlob(x, y, vx, vy, mass, fill, uuid, childUUID, game));

        this.mouseEvent = null;
        this.keyEvents = new ConcurrentHashMap<>();

        this.uuid = uuid;
        this.identifyPacket = identifyPacket;

        this.game = game;
    }

    public Player(UUID uuid, IdentifyPacket identifyPacket, Game game) {
        this(
            Utilities.random.nextDouble(PhysicsMap.width),
            Utilities.random.nextDouble(PhysicsMap.height),
            0, 0, 1_000,
            Utilities.randomColor(), uuid, identifyPacket, game
        );
    }

    public double getX() {
        return
            playerBlobs.values().stream().map(blob -> blob.mass * blob.x).reduce(0.0, Double::sum) /
            playerBlobs.values().stream().map(blob -> blob.mass).reduce(0.0, Double::sum);
    }

    public double getY() {
        return
            playerBlobs.values().stream().map(blob -> blob.mass * blob.y).reduce(0.0, Double::sum) /
            playerBlobs.values().stream().map(blob -> blob.mass).reduce(0.0, Double::sum);
    }

    public void keypressTicks(long time) {
        if (mouseEvent != null) {
            keyEvents.forEach((key, value) -> {
                if (value) {
                    switch (key) {
                        case KeyEvent.VK_W -> playerPellet(time);
                        case KeyEvent.VK_SPACE -> playerSplit(time, false, null);
                    }
                }
            });
        }
    }

    public void positionTick() {
        playerBlobs.values().forEach(playerBlob -> playerBlob.positionTick(this, mouseEvent));
    }

    public void collisionTick(long time) {
        ArrayList<Blob> allBlobs = ConsolidateBlobs.convert(
                game.viruses, game.foods, game.pellets
        ); //.stream().filter(Blob::isVisible).collect(Collectors.toCollection(ArrayList::new));

        for (int i = playerBlobs.size() - 1; i >= 0; --i) {
            ArrayList<UUID> uuidList = new ArrayList<>(playerBlobs.keySet());

            PlayerBlob playerBlob = playerBlobs.get(uuidList.get(i));

            ArrayList<Blob> collideBlobs = allBlobs
                .stream()
                .filter(blob -> Utilities.checkCollision(blob, playerBlob))
                .collect(Collectors.toCollection(ArrayList::new));

            playerBlob.tickDelay(time);
            playerBlob.collisionTick();

            for (int j = playerBlobs.size() - 1; j >= 0; --j) {
                PlayerBlob checkBlob = playerBlobs.get(uuidList.get(j));

                if (playerBlob.uuid != checkBlob.uuid) {
                    if (
                        playerBlob.cooldowns.merge < time &&
                        checkBlob.cooldowns.merge < time
                    ) {
                        if (checkCollision(playerBlob, checkBlob) && j > i) {
                            playerBlob.mass += checkBlob.mass;

                            playerBlobs.remove(uuidList.get(j));
                        }
                    } else if (
                        checkTouch(playerBlob, checkBlob) &&
                        checkBlob.mass <= playerBlob.mass
                    ) {
                        double[] pos = repositionBlob(playerBlob, checkBlob);

                        checkBlob.setX(pos[0]);
                        checkBlob.setY(pos[1]);
                    }
                }
            }

            for (int j = collideBlobs.size() - 1; j >= 0; --j) {
                Blob blob = collideBlobs.get(j);

                double rDiff = blob.getPhysicsRadius() - playerBlob.getPhysicsRadius();

                if (checkCollision(playerBlob, blob) && rDiff < 0) {
                    switch (blob.getType()) {
                        case FOOD -> playerBlob.mass += blob.mass;
                        case PELLET -> playerBlob.mass += pelletConsumeMass;
                        case SPIKE -> {
                            playerBlob.mass += virusConsumeMass;

                            if (playerBlobs.size() < playerMaxSplits) {
                                for (int k = 0; k < (playerMaxSplits - playerBlobs.size()) * 2 / 3; ++k) {
                                    playerSplit(time, true, playerBlob);
                                }
                            }
                        }
                        case PLAYER -> {} // TODO: doesn't work? needs separate loop
                        default -> System.out.println("Unknown blob interaction type: " + blob.getType());
                    }

                    blob.removeFromMap();
                }
            }
        }
    }

    private void playerSplit(long time, boolean wasSpike, PlayerBlob spikedBlob) {
        if (playerBlobs.size() >= playerMaxSplits) {
            return;
        }

        int maxSplit = playerBlobs.size() - 1;
        double spikedSplitSize = 0;

        if (wasSpike) {
            maxSplit = (int) (spikedBlob.mass / playerMinSplitSize);
            spikedSplitSize = spikedBlob.mass / maxSplit;
        }

        ArrayList<UUID> uuidList = new ArrayList<>(
            playerBlobs.values()
                .stream()
                .sorted((blob1, blob2) -> (int) (blob2.mass - blob1.mass))
                .map(blob -> blob.uuid)
                .toList()
        );

        for (int i = maxSplit; i >= 0; --i) {
            PlayerBlob playerBlob = wasSpike ? spikedBlob : playerBlobs.get(uuidList.get(i));

            if (
                playerBlob.mass < playerMinSplitSize ||
                (!wasSpike && playerBlob.cooldowns.split + playerSplitCooldown > time)
            ) {
                continue;
            }

            double explosionDelta = wasSpike
                    ? 360.0 / maxSplit * i
                    : Math.atan2(
                    mouseEvent.y() - playerBlob.getRelativeY(this),
                    mouseEvent.x() - playerBlob.getRelativeX(this)
                );

            double splitSize = wasSpike ? spikedSplitSize : playerBlob.mass / 2;

            playerBlob.mass -= splitSize;

            double splitRadius = Math.sqrt(splitSize / Math.PI);

            double[] pos = repositionBlob(playerBlob, splitRadius, explosionDelta);

            UUID childUUID = UUID.randomUUID();
            playerBlobs.put(childUUID, new PlayerBlob(pos[0], pos[1], splitSize, true, playerBlob.fill, uuid, childUUID, game));

            playerBlobs.get(childUUID).cooldowns.split = time;
            playerBlob.cooldowns.split = time;

            playerBlobs.get(childUUID).cooldowns.merge = time + calcMergeCooldown(playerBlobs.get(childUUID).mass);

            long playerMergeCD = calcMergeCooldown(playerBlob.mass);

            if (playerBlob.cooldowns.merge < time) {
                playerBlob.cooldowns.merge = time + playerMergeCD;
            } else {
                playerBlob.cooldowns.merge += playerMergeCD;
            }
        }
    }

    private void playerPellet(long time) {
        playerBlobs.values().forEach(playerBlob -> {
            if (
                playerBlob.mass < playerMinSizePellet ||
                playerBlob.cooldowns.pellet + pelletCooldown > time
            ) {
                return;
            }

            double delta = Math.atan2(
                mouseEvent.y() - playerBlob.getRelativeY(this),
                mouseEvent.x() - playerBlob.getRelativeX(this)
            );

            playerBlob.mass -= pelletMass;

            double pelletRadius = Math.sqrt(pelletMass / Math.PI);

            double[] pos = repositionBlob(playerBlob, pelletRadius, delta);

            UUID pelletUUID = UUID.randomUUID();
            game.pellets.put(pelletUUID, new Pellet(pos[0], pos[1], delta, pelletMass, playerBlob.fill, game, pelletUUID));

            playerBlob.cooldowns.pellet = time;
        });
    }

    public double getMass() {
        return playerBlobs.values().stream().mapToDouble(blob -> blob.mass).sum();
    }

    public void updateMouseEvent(MousePacket mouseEvent) {
        this.mouseEvent = mouseEvent;
    }

    public void updateKeyEvent(KeyPacket keyEvent) {
        keyEvents.put(keyEvent.keycode(), keyEvent.pressed());
    }

    public boolean visibilityCulling(Camera camera) {
        return playerBlobs.values().stream().anyMatch(playerBlob -> playerBlob.visibilityCulling(camera));
    }

    public JSONObject toJSON() {
        JSONArray blobArray = new JSONArray(playerBlobs.values().stream().map(PlayerBlob::toJSON).toList());

        return new JSONObject(String.format(
            "{\"uuid\":\"%s\",\"player_blobs\":%s}",
            uuid,
            blobArray
        ));
    }

}
