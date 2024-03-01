package ceccs.game.objects;

import ceccs.game.objects.elements.Player;

public class Camera {

    final private double screenWidth;
    final private double screenHeight;

    final private double x;
    final private double y;

    final protected double scale;

    public Camera(Player player) {
        this.screenWidth = player.identifyPacket.screenWidth;
        this.screenHeight = player.identifyPacket.screenHeight;

        this.scale = calculateCameraScale(player.getMass());
        this.x = player.getX() - screenWidth / this.scale / 2;
        this.y = player.getY() - screenHeight / this.scale / 2;
    }

    private double calculateCameraScale(double mass) {
        double dv = 50_000 / 300.0;
        double n = Math.log(dv) / Math.log(10) / 3;
        double A = 300 * Math.pow(10, 4 * n);

        double screenFactor = A / Math.pow(mass, n);

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

}
