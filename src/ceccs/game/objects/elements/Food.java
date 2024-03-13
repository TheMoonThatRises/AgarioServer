package ceccs.game.objects.elements;

import ceccs.game.Game;
import ceccs.game.objects.BLOB_TYPES;
import ceccs.game.utils.PhysicsMap;
import ceccs.game.utils.Utilities;

import java.util.UUID;

import static ceccs.game.configs.FoodConfigs.foodMaxSize;
import static ceccs.game.configs.FoodConfigs.foodMinSize;

public class Food extends Blob {

    public Food(Game game, UUID uuid) {
        super(
                Utilities.random.nextDouble(PhysicsMap.width),
                Utilities.random.nextDouble(PhysicsMap.height),
                foodMaxSize > foodMinSize ? Utilities.random.nextDouble(foodMinSize, foodMaxSize) : foodMaxSize,
                Utilities.randomColor(), uuid, game.foods
        );
    }

    @Override
    public BLOB_TYPES getType() {
        return BLOB_TYPES.FOOD;
    }

}
