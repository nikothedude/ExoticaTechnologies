package exoticatechnologies.modifications.upgrades;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.util.MagicSettings;
import exoticatechnologies.modifications.ShipModFactory;
import exoticatechnologies.util.Utilities;
import lombok.extern.log4j.Log4j;

import java.util.List;
import java.util.Map;
import java.util.Random;

@Log4j
public class UpgradesGenerator {
    //per fleet member!
    private static final float CHANCE_OF_UPGRADES = 0.4f;

    public static ETUpgrades generate(ShipVariantAPI var, String faction, float bandwidth) {
        Map<String, Float> factionUpgradeChances = MagicSettings.getFloatMap("exoticatechnologies", "factionUpgradeChances");
        Map<String, Float> factionPerUpgradeMult = MagicSettings.getFloatMap("exoticatechnologies", "factionPerUpgradeMult");

        List<Upgrade> allowedUpgrades = UpgradesHandler.getAllowedUpgrades(faction, var);
        if (allowedUpgrades.isEmpty()) {
            return null;
        }

        ETUpgrades upgrades = new ETUpgrades();

        float upgradeChance = CHANCE_OF_UPGRADES;
        if (factionUpgradeChances.containsKey(faction)) {
            upgradeChance = factionUpgradeChances.get(faction);
        }

        int smodCount = Utilities.getSModCount(var);
        upgradeChance *= (1 + smodCount);

        Random random = ShipModFactory.getRandom();
        if (random.nextFloat() < upgradeChance) {
            float perUpgradeMult = 1.0f;
            if (factionPerUpgradeMult.containsKey(faction)) {
                perUpgradeMult = factionPerUpgradeMult.get(faction);
            }
            perUpgradeMult *= (1 + smodCount * 0.5f);

            ShipAPI.HullSize hullSize = var.getHullSpec().getHullSize();
            WeightedRandomPicker<Upgrade> upgradePicker = getPicker(random, allowedUpgrades);

            while (random.nextFloat() < (bandwidth / 100f * perUpgradeMult)) {
                Upgrade upgrade = null;

                while (upgrade == null && !upgradePicker.isEmpty()) {
                    upgrade = upgradePicker.pick();

                    if (!(upgrade.getMaxLevel(hullSize) > upgrades.getUpgrade(upgrade)
                            && upgrade.canApply(var)
                            && (bandwidth - upgrade.getBandwidthUsage()) > 0f)) {
                        upgradePicker.remove(upgrade);
                        upgrade = null;
                    }
                    //has a chance to bias towards upgrades that already exist.
                    else if (upgrades.getUpgrade(upgrade) == 0
                            && random.nextFloat() > Math.max(0.05f, (0.8f - smodCount * 0.25f))) {
                        upgrade = null;
                    }
                }

                if (upgrade != null) {
                    if (random.nextFloat() < (upgrade.getSpawnChance() * (1 + 0.2f * smodCount))) {
                        upgrades.addUpgrades(upgrade, 1);
                    }

                    bandwidth = bandwidth - upgrade.getBandwidthUsage();
                } else {
                    break;
                }
            }
        }

        return upgrades;
    }

    public static ETUpgrades generate(FleetMemberAPI fm, String faction, float bandwidth) {
        Map<String, Float> factionUpgradeChances = MagicSettings.getFloatMap("exoticatechnologies", "factionUpgradeChances");
        Map<String, Float> factionPerUpgradeMult = MagicSettings.getFloatMap("exoticatechnologies", "factionPerUpgradeMult");

        List<Upgrade> allowedUpgrades = UpgradesHandler.getAllowedUpgrades(fm);
        if (allowedUpgrades.isEmpty()) {
            return null;
        }

        ETUpgrades upgrades = new ETUpgrades();

        float upgradeChance = CHANCE_OF_UPGRADES;
        if (faction != null && factionUpgradeChances.containsKey(faction)) {
            upgradeChance = factionUpgradeChances.get(faction);
        }

        int smodCount = Utilities.getSModCount(fm);
        upgradeChance *= (1 + smodCount);


        Random random = ShipModFactory.getRandom();
        if (random.nextFloat() < upgradeChance) {
            float perUpgradeMult = 1.0f;
            if (faction != null && factionPerUpgradeMult.containsKey(faction)) {
                perUpgradeMult = factionPerUpgradeMult.get(faction);
            }
            perUpgradeMult *= (1 + smodCount * 0.5f);

            ShipAPI.HullSize hullSize = fm.getHullSpec().getHullSize();
            WeightedRandomPicker<Upgrade> upgradePicker = getPicker(random, allowedUpgrades);

            while (random.nextFloat() < (bandwidth / 100f * perUpgradeMult)) {
                Upgrade upgrade = null;

                while (upgrade == null && !upgradePicker.isEmpty()) {

                    upgrade = upgradePicker.pick();

                    if (!(upgrade.getMaxLevel(hullSize) > upgrades.getUpgrade(upgrade)
                            && upgrade.canApply(fm)
                            && (bandwidth - upgrade.getBandwidthUsage()) > 0f)) {
                        upgradePicker.remove(upgrade);
                        upgrade = null;
                    }
                    //has a chance to bias towards upgrades that already exist.
                    //this chance ramps up as the ship gets more SMODs.
                    else if (upgrades.getUpgrade(upgrade) == 0
                            && random.nextFloat() > Math.max(0.05f, (0.8f - smodCount * 0.25f))) {
                        upgrade = null;
                    }
                }

                if (upgrade != null) {
                    if (random.nextFloat() < (upgrade.getSpawnChance() / 100f * (1 + 0.2f * smodCount))) {
                        upgrades.addUpgrades(upgrade, 1);
                    }

                    bandwidth = bandwidth - upgrade.getBandwidthUsage();
                } else if (upgradePicker.isEmpty()) {
                    break;
                }
            }
        }

        return upgrades;
    }

    private static WeightedRandomPicker<Upgrade> getPicker(Random random, List<Upgrade> allowedUpgrades) {
        WeightedRandomPicker<Upgrade> upgradePicker = new WeightedRandomPicker<>(random);

        for (Upgrade upgrade : allowedUpgrades) {
            upgradePicker.add(upgrade, upgrade.getSpawnChance());
        }

        return upgradePicker;
    }
}
