package ceccs.game.objects.elements;

import ceccs.utils.InternalException;
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
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static ceccs.utils.InternalException.checkSafeDivision;
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

        protected double relX;
        protected double relY;

        protected boolean hasSplitSpeedBoost;

        protected double splitBoostVelocity;

        final protected Cooldowns cooldowns;

        protected long lastDecayTick;

        public PlayerBlob(double x, double y, double vx, double vy, double ax, double ay, double mass, boolean hasSplitSpeedBoost, Paint fill, UUID parentUUID, UUID uuid, AbstractMap<UUID, PlayerBlob> parentMap) {
            super(x, y, vx, vy, ax, ay, mass, fill, uuid, parentMap);

            this.maxVx = 0;
            this.maxVy = 0;

            this.relX = 0;
            this.relY = 0;

            this.hasSplitSpeedBoost = hasSplitSpeedBoost;
            this.splitBoostVelocity = playerSplitVelocity;

            this.cooldowns = new Cooldowns();

            this.parentUUID = parentUUID;

            this.lastDecayTick = 0;
        }

        public PlayerBlob(double x, double y, double vx, double vy, double mass, Paint fill, UUID parentUUID, UUID uuid, AbstractMap<UUID, PlayerBlob> parentMap) {
            this(x, y, vx, vy, playerMouseAcc, playerMouseAcc, mass, false, fill, parentUUID, uuid, parentMap);
        }

        public PlayerBlob(double x, double y, double mass, boolean hasSplitSpeedBoost, Paint fill, UUID parentUUID, UUID uuid, AbstractMap<UUID, PlayerBlob> parentMap) {
            this(x, y, 0, 0, playerMouseAcc, playerMouseAcc, mass, hasSplitSpeedBoost, fill, parentUUID, uuid, parentMap);
        }

        @Override
        public BLOB_TYPES getType() {
            return BLOB_TYPES.PLAYER;
        }

        public void positionTick(Player player, MousePacket mouseEvent) {
            if (mouseEvent != null) {
                relX = mouseEvent.x() - getRelativeX(player);
                relY = mouseEvent.y() - getRelativeY(player);

                maxVx = playerVelocities[closestNumber(playerVelocities, relX / 1000)];
                maxVy = playerVelocities[closestNumber(playerVelocities, relY / 1000)];
            }

            double velScale;

            try {
                velScale = calcVelocityModifier(mass);
            } catch (InternalException exception) {
                exception.printStackTrace();

                System.err.println("mass is zero in position tick");

                return;
            }

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

            if (hasSplitSpeedBoost) {
                splitBoostVelocity -= playerSplitDecay;

                if (splitBoostVelocity <= 0) {
                    hasSplitSpeedBoost = false;
                } else {
                    double theta = Math.atan2(relY, relX);

                    double sVX = splitBoostVelocity * Math.cos(theta);
                    double sVY = splitBoostVelocity * Math.sin(theta);

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
            Camera camera = player.getCamera();

            return (x - camera.getX()) * camera.getScale();
        }

        public double getRelativeY(Player player) {
            Camera camera = player.getCamera();

            return (y - camera.getY()) * camera.getScale();
        }
    }

    final public UUID uuid;
    final public IdentifyPacket identifyPacket;

    final protected ConcurrentHashMap<UUID, PlayerBlob> playerBlobs;

    protected MousePacket mouseEvent;
    final protected ConcurrentHashMap<Integer, Boolean> keyEvents;

    final protected Game game;

    protected Camera prevCamera;

    public Player(double x, double y, double vx, double vy, double mass, Paint fill, UUID uuid, IdentifyPacket identifyPacket, Game game) {
        this.playerBlobs = new ConcurrentHashMap<>();

        UUID childUUID = UUID.randomUUID();

        this.playerBlobs.put(childUUID, new PlayerBlob(x, y, vx, vy, mass, fill, uuid, childUUID, this.playerBlobs));

        this.mouseEvent = null;
        this.keyEvents = new ConcurrentHashMap<>();

        this.uuid = uuid;
        this.identifyPacket = identifyPacket;

        this.game = game;

        this.prevCamera = this.getCamera();
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
        double numerator = playerBlobs.values().stream().map(blob -> blob.mass * blob.x).reduce(0.0, Double::sum);
        double denominator = playerBlobs.values().stream().map(blob -> blob.mass).reduce(0.0, Double::sum);

        try {
            return checkSafeDivision(numerator, denominator);
        } catch (InternalException exception) {
            exception.printStackTrace();

            System.err.println("failed to get player x");

            return getLegacyX();
        }
    }

    public double getY() {
        double numerator = playerBlobs.values().stream().map(blob -> blob.mass * blob.y).reduce(0.0, Double::sum);
        double denominator = playerBlobs.values().stream().map(blob -> blob.mass).reduce(0.0, Double::sum);

        try {
            return checkSafeDivision(numerator, denominator);
        } catch (InternalException exception) {
            exception.printStackTrace();

            System.err.println("failed to get player y");

            return getLegacyY();
        }
    }

    protected double getLegacyX() {
        return playerBlobs.values().stream().max(Comparator.comparingDouble(b -> b.mass)).get().getX();
    }

    protected double getLegacyY() {
        return playerBlobs.values().stream().max(Comparator.comparingDouble(b -> b.mass)).get().getY();
    }

    public void keypressTicks(long time) {
        if (mouseEvent != null) {
            keyEvents.forEach((key, value) -> {
                if (value) {
                    switch (key) {
                        case KeyEvent.VK_W -> playerPellet(time);
                        case KeyEvent.VK_SPACE -> {
                            try {
                                playerSplit(time, false, null);
                            } catch (InternalException exception) {
                                exception.printStackTrace();

                                System.err.println("failed to split");
                            }
                        }
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
        );

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

                            checkBlob.removeFromMap();
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
                                try {
                                    playerSplit(time, true, playerBlob);
                                } catch (InternalException exception) {
                                    exception.printStackTrace();

                                    System.err.println("failed to split");
                                }
                            }
                        }
                        case PLAYER -> {} // TODO: doesn't work? needs separate loop
                        default -> System.out.println("unknown blob interaction type: " + blob.getType());
                    }

                    blob.removeFromMap();
                }
            }
        }
    }

    private void playerSplit(long time, boolean wasSpike, PlayerBlob spikedBlob) throws InternalException {
        if (playerBlobs.size() >= playerMaxSplits) {
            return;
        }

        int maxSplit = playerBlobs.size() - 1;
        double spikedSplitSize = 0;
        double splitCircumference = 0;

        if (wasSpike) {
            maxSplit = (int) (spikedBlob.mass / playerMinSplitSize) - 1;

            if (maxSplit <= 0) {
                throw new InternalException("unsafe value: max split = " + maxSplit);
            }

            spikedSplitSize = spikedBlob.mass / maxSplit;
            splitCircumference = Math.sqrt(spikedSplitSize / Math.PI) * maxSplit / Math.PI;

            spikedBlob.mass = spikedSplitSize;
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

            double explosionTheta = wasSpike
                ? (360.0 / maxSplit) * i
                : Math.atan2(
                    mouseEvent.y() - playerBlob.getRelativeY(this),
                    mouseEvent.x() - playerBlob.getRelativeX(this)
                );

            double splitSize = wasSpike ? spikedSplitSize : playerBlob.mass / 2;

            if (!wasSpike) {
                playerBlob.mass -= splitSize;
            }

            if (playerBlob.mass <= 0) {
                throw new InternalException("player blob mass less than zero: mass = " + playerBlob.mass);
            }

            double splitRadius = wasSpike ? splitCircumference : Math.sqrt(splitSize / Math.PI);

            double[] pos = repositionBlob(playerBlob, splitRadius, explosionTheta);

            if (Double.isNaN(pos[0]) || Double.isNaN(pos[1])) {
                try {
                    throw new InternalException("x or y is NaN: x = " + pos[0] + ", y = " + pos[1]);
                } catch (InternalException exception) {
                    exception.printStackTrace();

                    System.err.println("x or y pos are NaN");

                    pos[0] = spikedBlob.x;
                    pos[1] = spikedBlob.y;
                }
            }

            UUID childUUID = UUID.randomUUID();

            PlayerBlob newBlob = new PlayerBlob(pos[0], pos[1], splitSize, true, playerBlob.fill, uuid, childUUID, playerBlobs);
            playerBlobs.put(childUUID, newBlob);

            newBlob.cooldowns.split = time;
            playerBlob.cooldowns.split = time;

            newBlob.cooldowns.merge = time + calcMergeCooldown(newBlob.mass);

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

            double theta = Math.atan2(
                mouseEvent.y() - playerBlob.getRelativeY(this),
                mouseEvent.x() - playerBlob.getRelativeX(this)
            );

            playerBlob.mass -= pelletMass;

            double pelletRadius = Math.sqrt(pelletMass / Math.PI);

            double[] pos = repositionBlob(playerBlob, pelletRadius, theta);

            UUID pelletUUID = UUID.randomUUID();
            game.pellets.put(pelletUUID, new Pellet(pos[0], pos[1], theta, pelletMass, playerBlob.fill, game, pelletUUID));

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

    public Camera getCamera() {
        try {
            Camera camera = new Camera(this);

            prevCamera = camera;

            return camera;
        } catch (InternalException exception) {
            exception.printStackTrace();

            System.err.println("failed to get player camera");

            return prevCamera;
        }
    }

    public boolean visibilityCulling() {
        return playerBlobs.values().stream().anyMatch(playerBlob -> playerBlob.visibilityCulling(getCamera()));
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
