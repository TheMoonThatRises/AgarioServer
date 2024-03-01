package ceccs.game.objects.elements;

import ceccs.game.Game;
import ceccs.game.objects.BLOB_TYPES;
import javafx.scene.paint.Paint;

import java.util.UUID;

import static ceccs.game.configs.PelletConfigs.pelletFriction;
import static ceccs.game.configs.PelletConfigs.pelletVelocity;

public class Pellet extends Blob {

    final private double projected;
    private int time;

    private boolean didFinish;

    public Pellet(double x, double y, double theta, double mass, Paint fill, Game game, UUID uuid) {
        super(x, y, 0, 0, 0, 0, mass, fill, game, uuid, game.pellets);

        this.projected = pelletVelocity / pelletFriction;
        this.time = 0;

        this.vx = pelletVelocity * Math.cos(theta);
        this.vy = pelletVelocity * Math.sin(theta);

        this.ax = -pelletFriction * Math.cos(theta);
        this.ay = -pelletFriction * Math.sin(theta);

        this.didFinish = false;
    }

    @Override
    public BLOB_TYPES getType() {
        return BLOB_TYPES.PELLET;
    }

    @Override
    public void positionTick() {
        if (!didFinish) {
            ++time;

            super.positionTick();

            if (time >= projected) {
                ax = ay = vx = vy = 0;
                didFinish = true;
            }
        }
    }

}
