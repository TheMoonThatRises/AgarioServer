package ceccs.game.objects.elements;

import ceccs.game.Game;
import ceccs.game.objects.BLOB_TYPES;
import ceccs.game.objects.Camera;
import ceccs.game.utils.PhysicsMap;
import ceccs.game.utils.Utilities;
import javafx.scene.paint.Paint;
import org.json.JSONObject;

import java.util.AbstractMap;
import java.util.UUID;

public class Blob {

    final public UUID uuid;

    protected double x;
    protected double y;
    protected double vx;
    protected double vy;
    protected double ax;
    protected double ay;

    final protected double initialVx;
    final protected double initialVy;

    protected double mass;

    protected Paint fill;

    protected AbstractMap<UUID, ? extends Blob> parentMap;

    public Blob(double x, double y, double vx, double vy, double ax, double ay, double mass, Paint fill, UUID uuid, AbstractMap<UUID, ? extends Blob> parentMap) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.ax = ax;
        this.ay = ay;
        this.mass = mass;

        this.initialVx = vx;
        this.initialVy = vy;

        this.parentMap = parentMap;

        this.uuid = uuid;

        this.fill = fill;
    }

    public Blob(double x, double y, double mass, Paint fill, UUID uuid, AbstractMap<UUID, ? extends Blob> parentMap) {
        this(x, y, 0, 0, 0, 0, mass, fill, uuid, parentMap);
    }

    public BLOB_TYPES getType() {
        return BLOB_TYPES.GENERIC;
    }

    public void removeFromMap() {
        parentMap.remove(uuid);
    }

    public void positionTick() {
        vx += ax;
        vy += ay;

        x += vx;
        y += vy;
    }

    public void collisionTick() {
        double radius = getPhysicsRadius();

        if (x - radius < 0) {
            x = radius;
            vx = 0;
        } else if (x + radius > PhysicsMap.width) {
            x = PhysicsMap.width - radius;
            vx = 0;
        }

        if (y - radius < 0) {
            y = radius;
            vy = 0;
        } else if (y + radius > PhysicsMap.height) {
            y = PhysicsMap.height - radius;
            vy = 0;
        }
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getPhysicsRadius() {
        return Math.sqrt(mass / Math.PI);
    }

    public boolean visibilityCulling(Camera camera) {
        double relX = (x - camera.getX()) * camera.getScale();
        double relY = (y - camera.getY()) * camera.getScale();
        double relRadius = getPhysicsRadius() * camera.getScale();

        boolean defaultVisibility = !(
                relX + relRadius < -10 ||
                relX - relRadius > camera.getScreenWidth() + 10 ||
                relY + relRadius < -10 ||
                relY - relRadius > camera.getScreenHeight() + 10 ||
                relRadius < 0.5
        );

        return camera.getScale() < 3 && defaultVisibility && mass < 100
            ? Utilities.random.nextDouble() > 0.6
            : defaultVisibility;
    }

    public JSONObject toJSON() {
        return new JSONObject(String.format(
            "{\"uuid\":\"%s\",\"x\":%f,\"y\":%f,\"vx\":%f,\"vy\":%f,\"ax\":%f,\"ay\":%f,\"mass\":%f,\"fill\":\"%s\"}",
            uuid, x, y, vx, vy, ax, ay, mass, fill.toString()
        ));
    }

}
