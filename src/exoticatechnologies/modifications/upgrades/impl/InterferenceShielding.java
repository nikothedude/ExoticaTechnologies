package exoticatechnologies.modifications.upgrades.impl;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import exoticatechnologies.modifications.ShipModifications;
import exoticatechnologies.modifications.upgrades.Upgrade;
import exoticatechnologies.util.StatUtils;
import exoticatechnologies.util.StringUtils;
import lombok.Getter;

import java.awt.*;

public class InterferenceShielding extends Upgrade {
    @Getter protected final float bandwidthUsage = 10f;
    private static final float SENSOR_PROFILE_MULT = -50f;
    private static final float SUPPLY_CONSUMPTION_MULT = 16f;
    private static final Color COLOR = new Color(93, 59, 210);

    @Override
    public Color getColor() {
        return COLOR;
    }

    @Override
    public void applyUpgradeToStats(FleetMemberAPI fm, MutableShipStatsAPI stats, int level, int maxLevel) {
        StatUtils.setStatMult(stats.getSensorProfile(), this.getBuffId(), level, -Math.abs(SENSOR_PROFILE_MULT), maxLevel);

        if (level >= 3) {
            StatUtils.setStatPercent(stats.getSuppliesPerMonth(), this.getBuffId(), level, SUPPLY_CONSUMPTION_MULT, maxLevel);
        }
    }

    @Override
    public void printStatInfoToTooltip(TooltipMakerAPI tooltip, FleetMemberAPI fm, ShipModifications mods) {
        this.addBenefitToShopTooltipMult(tooltip, "sensorProfile", fm, mods, SENSOR_PROFILE_MULT);

        StringUtils.getTranslation("UpgradesDialog", "UpgradeDrawbackAfterLevel")
                .format("level", 3)
                .addToTooltip(tooltip);

        this.addMalusToShopTooltip(tooltip, "supplyConsumption", fm, mods, 3, SUPPLY_CONSUMPTION_MULT);
    }

    @Override
    public void modifyToolTip(TooltipMakerAPI tooltip, FleetMemberAPI fm, ShipModifications systems, boolean expand) {
        int level = systems.getUpgrade(this);

        if (expand) {
            tooltip.addPara(this.getName() + " (%s):", 5, this.getColor(), String.valueOf(level));

            float sensorProfileBonus = fm.getStats().getSensorProfile().getMultStatMod(this.getBuffId()).getValue();
            this.addBenefitToTooltipMult(tooltip,
                    "sensorProfile",
                    sensorProfileBonus,
                    fm.getStats().getSensorProfile().getBaseValue());

            MutableStat.StatMod supplyConsumptionStat = fm.getStats().getSuppliesPerMonth().getPercentStatMod(this.getBuffId());
            float supplyConsumptionBonus = 0f;
            if (supplyConsumptionStat != null) {
                supplyConsumptionBonus = supplyConsumptionStat.getValue();
            }

            this.addMalusToTooltip(tooltip,
                    "supplyConsumption",
                    supplyConsumptionBonus,
                    fm.getVariant().getHullSpec().getSuppliesPerMonth());
        } else {
            tooltip.addPara(this.getName() + " (%s)", 5, this.getColor(), String.valueOf(level));
        }
    }

    @Override
    public float getSpawnChance() {
        return super.getSpawnChance() * 0.33f;
    }
}
