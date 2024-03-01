package ceccs.game.objects.elements;

import ceccs.game.Game;
import ceccs.game.objects.BLOB_TYPES;
import ceccs.game.utils.PhysicsMap;
import ceccs.game.utils.Utilities;
import javafx.scene.paint.Color;

import java.util.UUID;

import static ceccs.game.configs.VirusConfigs.virusMass;

public class Virus extends Blob {

    public Virus(Game game, UUID uuid) {
        super(
                Utilities.random.nextDouble(PhysicsMap.width),
                Utilities.random.nextDouble(PhysicsMap.height),
                virusMass, Color.GREEN, game, uuid, game.viruses
        );
    }

    @Override
    public BLOB_TYPES getType() {
        return BLOB_TYPES.SPIKE;
    }

}
