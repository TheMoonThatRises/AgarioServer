package ceccs.game.objects;

import ceccs.game.objects.elements.Player;
import ceccs.utils.InternalException;
import org.json.JSONObject;

public class Camera {

    final protected double scale;
    final private double screenWidth;
    final private double screenHeight;
    final private double x;
    final private double y;

    public Camera(Player player) throws InternalException {
        this.screenWidth = player.identifyPacket.screenWidth();
        this.screenHeight = player.identifyPacket.screenHeight();

        this.scale = calculateCameraScale(player.getMass());

        this.x = player.getX() - screenWidth / this.scale / 2;
        this.y = player.getY() - screenHeight / this.scale / 2;
    }

    private double calculateCameraScale(double mass) throws InternalException {
        double dv = 50_000 / 300.0;
        double n = Math.log(dv) / Math.log(10) / 3;
        double A = 300 * Math.pow(10, 4 * n);

        double screenFactor = A / Math.pow(mass, n);

        if (mass == 0) {
            throw new InternalException("unsafe zero: mass = " + mass);
        }

        return (screenWidth * screenWidth * Math.PI) / (screenFactor * mass);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getScreenWidth() {
        return screenWidth;
    }

    public double getScreenHeight() {
        return screenHeight;
    }

    public double getScale() {
        return scale;
    }

    public JSONObject toJSON() {
        return new JSONObject()
                .put("x", x)
                .put("y", y)
                .put("scale", scale)
                .put("screen_width", screenWidth)
                .put("screen_height", screenHeight);
    }

}
