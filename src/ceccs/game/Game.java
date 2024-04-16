package ceccs.game;

import ceccs.game.objects.Camera;
import ceccs.game.objects.elements.Food;
import ceccs.game.objects.elements.Pellet;
import ceccs.game.objects.elements.Player;
import ceccs.game.objects.elements.Virus;
import ceccs.network.PlayerSocket;
import ceccs.network.data.IdentifyPacket;
import ceccs.network.data.KeyPacket;
import ceccs.network.data.MousePacket;
import ceccs.network.utils.CustomID;
import javafx.util.Pair;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import static ceccs.game.configs.FoodConfigs.maxFoodCount;
import static ceccs.game.configs.VirusConfigs.maxVirusCount;

public class Game {

    private long lastTps;
    private long tps;

    final public ConcurrentHashMap<CustomID, Player> players;
    final public ConcurrentHashMap<CustomID, Food> foods;
    final public ConcurrentHashMap<CustomID, Pellet> pellets;
    final public ConcurrentHashMap<CustomID, Virus> viruses;

    final private ArrayList<Pair<PlayerSocket, IdentifyPacket>> spawnQueue;
    final private ArrayList<CustomID> despawnQueue;

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

        this.lastTps = System.nanoTime();

        Game self = this;

        this.heartbeatTask = new TimerTask() {
            @Override
            public void run() {
                if (!spawnQueue.isEmpty()) {
                    // spawn all queued players
                    spawnQueue.forEach((playerInfo) ->
                            players.put(
                                    playerInfo.getKey().getID(),
                                    new Player(playerInfo.getKey().getID(), playerInfo.getValue(), self, playerInfo.getKey())
                            )
                    );
                    spawnQueue.clear();
                }

                if (!despawnQueue.isEmpty()) {
                    // despawn all queued players
                    despawnQueue.forEach(players::remove);
                    despawnQueue.clear();
                }

                // update physics
                players.values().stream().parallel().forEach(Player::positionTick);
                pellets.values().stream().parallel().forEach(Pellet::positionTick);
                viruses.values().stream().parallel().forEach(Virus::positionTick);

                players.forEach((id, player) -> player.collisionTick(System.nanoTime()));
                viruses.forEach((id, virus) -> virus.collisionTick());
                pellets.forEach((id, pellet) -> pellet.collisionTick());
//                foods.forEach((id, food) -> food.collisionTick());

                players.values().stream().parallel().forEach(player -> player.keypressTicks(System.nanoTime()));

                // load back items to env
                if (foods.size() < maxFoodCount) {
                    CustomID uuid = CustomID.randomID();

                    foods.put(uuid, new Food(self, uuid));
                }

                if (viruses.size() < maxVirusCount) {
                    CustomID uuid = CustomID.randomID();

                    viruses.put(uuid, new Virus(self, uuid));
                }

                tps = System.nanoTime() - lastTps;
                lastTps = System.nanoTime();
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
            CustomID uuid = CustomID.randomID();

            foods.put(uuid, new Food(this, uuid));
        }

        for (int i = 0; i < maxVirusCount; ++i) {
            CustomID uuid = CustomID.randomID();

            viruses.put(uuid, new Virus(this, uuid));
        }
    }

    public void spawnPlayer(PlayerSocket playerSocket, IdentifyPacket identifyPacket) {
        spawnQueue.add(new Pair<>(playerSocket, identifyPacket));
    }

    public void despawnPlayer(CustomID uuid) {
        despawnQueue.add(uuid);
    }

    public void updatePlayerMouse(CustomID uuid, MousePacket mouse) {
        players.get(uuid).updateMouseEvent(mouse);
    }

    public void updatePlayerKey(CustomID uuid, KeyPacket key) {
        players.get(uuid).updateKeyEvent(key);
    }

    public JSONObject getGameState(CustomID playerUUID) {
        Camera camera = players.get(playerUUID).getCamera();

        JSONObject gameData = new JSONObject();
        gameData.put("foods", foods.values().stream().parallel().filter(food ->
            food.visibilityCulling(camera)
        ).map(Food::toJSON).toList());
        gameData.put("pellets", pellets.values().stream().parallel().filter(pellets ->
            pellets.visibilityCulling(camera)
        ).map(Pellet::toJSON).toList());
        gameData.put("viruses", viruses.values().stream().parallel().filter(virus ->
            virus.visibilityCulling(camera)
        ).map(Virus::toJSON).toList());
        gameData.put("players", players.values().stream().parallel().filter(player ->
            player.visibilityCulling(camera)
        ).map(Player::toJSON).toList());

        return gameData;
    }

    public long getTps() {
        return tps;
    }

}
