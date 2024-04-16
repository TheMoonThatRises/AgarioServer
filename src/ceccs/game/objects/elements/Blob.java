package ceccs.game.objects.elements;

import ceccs.game.objects.BLOB_TYPES;
import ceccs.game.objects.Camera;
import ceccs.game.utils.PhysicsMap;
import ceccs.game.utils.Utilities;
import ceccs.network.utils.CustomID;
import javafx.scene.paint.Paint;
import org.json.JSONObject;

import java.util.AbstractMap;

public class Blob {

    final public CustomID uuid;

    protected double x;
    protected double y;
    protected double vx;
    protected double vy;
    protected double ax;
    protected double ay;

    protected double mass;

    protected Paint fill;

    protected AbstractMap<CustomID, ? extends Blob> parentMap;

    public Blob(double x, double y, double vx, double vy, double ax, double ay, double mass, Paint fill, CustomID uuid, AbstractMap<CustomID, ? extends Blob> parentMap) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.ax = ax;
        this.ay = ay;

        this.mass = mass;

        this.parentMap = parentMap;

        this.uuid = uuid;

        this.fill = fill;
    }

    public Blob(double x, double y, double mass, Paint fill, CustomID uuid, AbstractMap<CustomID, ? extends Blob> parentMap) {
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

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
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
        return new JSONObject()
                .put("uuid", uuid.toString())
                .put("x", x)
                .put("y", y)
                .put("vx", vx)
                .put("vy", vy)
                .put("ax", ax)
                .put("ay", ay)
                .put("mass", mass)
                .put("fill", fill.toString());
    }

}
