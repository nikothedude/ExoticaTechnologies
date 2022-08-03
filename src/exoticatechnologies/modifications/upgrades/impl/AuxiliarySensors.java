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

public class AuxiliarySensors extends Upgrade {
    @Getter protected final float bandwidthUsage = 5f;
    private static final float SENSOR_STRENGTH_MULT = 50f;
    private static final float SENSOR_PROFILE_MULT = 10f;
    private static final float MIN_CREW_MULT = 20f;
    private static final Color COLOR = new Color(178, 178, 178);

    @Override
    public Color getColor() {
        return COLOR;
    }

    @Override
    public void applyUpgradeToStats(FleetMemberAPI fm, MutableShipStatsAPI stats, int level, int maxLevel) {
        StatUtils.setStatPercent(stats.getSensorStrength(), this.getBuffId(), level, SENSOR_STRENGTH_MULT, maxLevel);

        if (level >= 3) {
            StatUtils.setStatPercent(stats.getSensorProfile(), this.getBuffId(), level, SENSOR_PROFILE_MULT, maxLevel);
            StatUtils.setStatPercent(stats.getMinCrewMod(), this.getBuffId(), level, MIN_CREW_MULT, maxLevel);
        }
    }

    @Override
    public void printStatInfoToTooltip(TooltipMakerAPI tooltip, FleetMemberAPI fm, ShipModifications mods) {
        this.addBenefitToShopTooltip(tooltip, "sensorStrength", fm, mods, SENSOR_STRENGTH_MULT);

        //after level 3
        StringUtils.getTranslation("ShipListDialog", "UpgradeDrawbackAfterLevel")
                .format("level", 3)
                .addToTooltip(tooltip);
        this.addMalusToShopTooltip(tooltip, "sensorProfile", fm, mods, 3, SENSOR_PROFILE_MULT);
        this.addMalusToShopTooltip(tooltip, "requiredCrew", fm, mods, 3, MIN_CREW_MULT);
    }

    @Override
    public void modifyToolTip(TooltipMakerAPI tooltip, FleetMemberAPI fm, ShipModifications systems, boolean expand) {
        int level = systems.getUpgrade(this);

        if (expand) {
            tooltip.addPara(this.getName() + " (%s):", 5, this.getColor(), String.valueOf(level));

            this.addBenefitToTooltip(tooltip,
                    "sensorStrength",
                    fm.getStats().getSensorStrength().getPercentStatMod(this.getBuffId()).getValue(),
                    fm.getStats().getSensorStrength().getBaseValue());

            MutableStat.StatMod sensorProfileStat = fm.getStats().getSensorProfile().getPercentStatMod(this.getBuffId());
            float sensorProfileBonus = 1f;
            if (sensorProfileStat != null) {
                sensorProfileBonus = sensorProfileStat.getValue();
            }

            this.addMalusToTooltip(tooltip,
                    "sensorProfile",
                    sensorProfileBonus,
                    fm.getStats().getSensorProfile().getBaseValue());

            MutableStat.StatMod minCrewStat = fm.getStats().getMinCrewMod().getPercentBonus(this.getBuffId());
            float minCrewBonus = 0f;
            if (minCrewStat != null) {
                minCrewBonus = minCrewStat.getValue();
            }

            this.addMalusToTooltip(tooltip,
                    "requiredCrew",
                    minCrewBonus,
                    fm.getVariant().getHullSpec().getMinCrew());
        } else {
            tooltip.addPara(this.getName() + " (%s)", 5, this.getColor(), String.valueOf(level));
        }
    }

    @Override
    public float getSpawnChance() {
        return super.getSpawnChance() * 0.33f;
    }
}
