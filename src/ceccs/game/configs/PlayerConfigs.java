package ceccs.game.configs;

public class PlayerConfigs {

    final static public double playerDefaultMass = 15;

    final static public int playerMaxSplits = 16;
    final static public double playerMinSplitSize = 35;
    final static public long playerSplitCooldown = 200_000_000;
    final static public double playerSplitVelocity = 4;
    final static public double playerSplitDecay = 0.1;

    final static public double[] playerVelocities = new double[] {
            -0.01, -0.05, -0.1, -0.2, -0.4, -0.5,
            0,
            0.01, 0.05, 0.1, 0.2, 0.4, 0.5
    };

    final static public double playerMouseAcc = 0.1;

    public static long calcMergeCooldown(double mass) {
        return 30_000_000_000L + (long) (mass * 0.02); // 30_000_000_000L
    }

    public static double calcVelocityModifier(double mass) {
        double dv = 2 / 0.1;
        double n = Math.log(dv) / Math.log(10) / 3;
        double A = 0.1 * Math.pow(10, 4 * n);

        return A / Math.pow(mass, n);
    }

}
