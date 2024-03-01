package ceccs.game.configs;

import ceccs.game.utils.PhysicsMap;

import static ceccs.game.configs.PelletConfigs.pelletMass;

public class VirusConfigs {

    final static public double virusMass = 132;
    final static public double virusConsumeMass = 100;

    final static public double virusCriticalMass = virusMass + (pelletMass * 4 / 3) * 7;

    final static public double maxVirusCount = (PhysicsMap.width * PhysicsMap.height) / (100 * 100);

}
