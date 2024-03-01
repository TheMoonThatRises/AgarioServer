package ceccs.game;

import ceccs.game.objects.Camera;
import ceccs.game.objects.elements.*;
import ceccs.game.utils.Utilities;
import ceccs.network.data.KeyPacket;
import ceccs.network.data.MousePacket;
import ceccs.network.data.IdentifyPacket;
import javafx.util.Pair;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static ceccs.game.configs.VirusConfigs.maxVirusCount;
import static ceccs.game.configs.FoodConfigs.maxFoodCount;

public class Game {

    final public ConcurrentHashMap<UUID, Player> players;
    final public ConcurrentHashMap<UUID, Food> foods;
    final public ConcurrentHashMap<UUID, Pellet> pellets;
    final public ConcurrentHashMap<UUID, Virus> viruses;

    final private ArrayList<Pair<UUID, IdentifyPacket>> spawnQueue;
    final private ArrayList<UUID> despawnQueue;

    final private TimerTask heartbeatTask;
    final private Timer heartbeat;

    public Game() {
        this.players = new ConcurrentHashMap<>();
        this.foods = new ConcurrentHashMap<>();
        this.pellets = new ConcurrentHashMap<>();
        this.viruses = new ConcurrentHashMap<>();

        this.spawnQueue = new ArrayList<>();
        this.despawnQueue = new ArrayList<>();

        this.heartbeat = new Timer("heartbeat");

        Game self = this;

        this.heartbeatTask = new TimerTask() {
            @Override
            public void run() {
                // spawn all queued players
                for (Pair<UUID, IdentifyPacket> playerInfo : spawnQueue) {
                    players.put(playerInfo.getKey(), new Player(playerInfo.getKey(), playerInfo.getValue(), self));
                }

                spawnQueue.clear();

                // despawn all queued players
                for (UUID uuid : despawnQueue) {
                    players.remove(uuid);
                }

                despawnQueue.clear();

                // update player physics
                players.forEach((id, player) -> player.positionTick());
                players.forEach((id, player) -> player.collisionTick(System.nanoTime()));
                players.forEach((id, player) -> player.keypressTicks(System.nanoTime()));

                // update env physics
                pellets.forEach((id, pellet) -> pellet.positionTick());

                viruses.forEach((id, virus) -> virus.collisionTick());
                pellets.forEach((id, pellet) -> pellet.collisionTick());
                foods.forEach((id, food) -> food.collisionTick());

                // load back items to env
                if (foods.size() < maxFoodCount) {
                    UUID uuid = UUID.randomUUID();

                    foods.put(uuid, new Food(self, uuid));
                }

                if (viruses.size() < maxVirusCount) {
                    UUID uuid = UUID.randomUUID();

                    viruses.put(uuid, new Virus(self, uuid));
                }
            }
        };
    }

    public void pauseHeartbeat() {
        heartbeat.cancel();
    }

    public void startHeartbeat() {
        heartbeat.scheduleAtFixedRate(heartbeatTask, 0, 10);
    }

    public void loadEnvironment() {
        for (int i = 0; i < maxFoodCount; ++i) {
            UUID uuid = UUID.randomUUID();

            foods.put(uuid, new Food(this, uuid));
        }

        for (int i = 0; i < maxVirusCount; ++i) {
            UUID uuid = UUID.randomUUID();

            viruses.put(uuid, new Virus(this, uuid));
        }
    }

    public void spawnPlayer(UUID uuid, IdentifyPacket identifyPacket) {
        spawnQueue.add(new Pair<>(uuid, identifyPacket));
    }

    public void despawnPlayer(UUID uuid) {
        despawnQueue.add(uuid);
    }

    public void updatePlayerMouse(UUID uuid, MousePacket mouse) {
        players.get(uuid).updateMouseEvent(mouse);
    }

    public void updatePlayerKey(UUID uuid, KeyPacket key) {
        players.get(uuid).updateKeyEvent(key);
    }

    public JSONObject getGameState(UUID playerUUID) {
        Camera camera = new Camera(players.get(playerUUID));

        JSONObject gameData = new JSONObject();
        gameData.put("foods", foods.values().stream().filter(food ->
                food.visibilityCulling(camera)
        ).map(Food::toJSON).toList());
        gameData.put("pellets", pellets.values().stream().filter(pellets ->
                pellets.visibilityCulling(camera)
        ).map(Pellet::toJSON).toList());
        gameData.put("viruses", viruses.values().stream().filter(virus ->
                virus.visibilityCulling(camera)
        ).map(Virus::toJSON).toList());
        gameData.put("players", players.values().stream().filter(player ->
                player.visibilityCulling(camera)
        ).map(Player::toJSON).toList());

        return gameData;
    }

}
