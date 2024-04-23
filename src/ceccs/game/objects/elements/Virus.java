package ceccs.game.objects.elements;

import ceccs.game.Game;
import ceccs.game.objects.BLOB_TYPES;
import ceccs.game.utils.PhysicsMap;
import ceccs.game.utils.Utilities;
import ceccs.network.utils.CustomID;
import ceccs.utils.InternalException;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.stream.Collectors;

import static ceccs.game.configs.VirusConfigs.*;
import static ceccs.game.utils.Utilities.checkCollision;
import static ceccs.game.utils.Utilities.repositionBlob;

public class Virus extends Blob {

    final private double projected;
    protected Game game;
    private int time;
    private boolean didFinish;

    public Virus(Game game, CustomID uuid) {
        super(
                Utilities.random.nextDouble(PhysicsMap.width),
                Utilities.random.nextDouble(PhysicsMap.height),
                virusMass, Color.GREEN, uuid, game.viruses
        );

        this.didFinish = true;
        this.projected = 0;
        this.time = 0;

        this.game = game;
    }

    public Virus(double x, double y, double theta, double mass, Game game, CustomID uuid) {
        super(
                x, y, mass, Color.GREEN, uuid, game.viruses
        );

        this.projected = virusVelocity / virusFriction;
        this.time = 0;

        this.vx = virusVelocity * Math.cos(theta);
        this.vy = virusVelocity * Math.sin(theta);

        this.ax = -virusFriction * Math.cos(theta);
        this.ay = -virusFriction * Math.sin(theta);

        this.didFinish = false;

        this.game = game;
    }

    @Override
    public BLOB_TYPES getType() {
        return BLOB_TYPES.SPIKE;
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

    @Override
    public void collisionTick() {
        super.collisionTick();

        ArrayList<Pellet> pellets = game.pellets.values()
                .stream()
                .filter(blob -> {
                    if (blob == null) {
                        return false;
                    }

                    try {
                        return checkCollision(blob, this);
                    } catch (InternalException exception) {
                        System.err.println("failed to check collision");

                        exception.printStackTrace();

                        return false;
                    }
                })
                .collect(Collectors.toCollection(ArrayList::new));

        for (int i = pellets.size() - 1; i >= 0; --i) {
            Pellet pellet = pellets.get(i);

            double rDiff = pellet.getPhysicsRadius() - getPhysicsRadius();

            try {
                if (checkCollision(this, pellet) && rDiff < 0) {
                    mass += pellet.mass;

                    if (mass >= virusCriticalMass) {
                        split(pellet);
                    }

                    pellet.removeFromMap();
                }
            } catch (InternalException exception) {
                System.err.println("virus collision tick failed check with pellet");

                exception.printStackTrace();
            }
        }
    }

    private void split(Pellet criticalPellet) {
        try {
            double theta = Math.atan2(
                    criticalPellet.vy,
                    criticalPellet.vx
            );

            mass /= 2;

            double[] pos = repositionBlob(this, getPhysicsRadius(), theta);

            CustomID splitUUID = CustomID.randomID();
            game.viruses.put(splitUUID, new Virus(pos[0], pos[1], theta, mass, game, splitUUID));
        } catch (InternalException exception) {
            System.err.println("virus failed to split");

            exception.printStackTrace();
        }
    }

}
