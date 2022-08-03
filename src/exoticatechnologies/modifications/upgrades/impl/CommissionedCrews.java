package exoticatechnologies.modifications.upgrades.impl;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.econ.MonthlyReport;
import com.fs.starfarer.api.campaign.listeners.EconomyTickListener;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exoticatechnologies.ETModPlugin;
import exoticatechnologies.integration.ironshell.IronShellIntegration;
import exoticatechnologies.modifications.ShipModFactory;
import exoticatechnologies.modifications.ShipModifications;
import exoticatechnologies.util.StatUtils;
import exoticatechnologies.modifications.upgrades.Upgrade;
import exoticatechnologies.util.StringUtils;
import lombok.Getter;

import java.awt.*;

public class CommissionedCrews extends Upgrade {
    @Getter protected final float bandwidthUsage = 5f;
    private static final int COST_PER_CREW_MAX = 25; //in addition to base crew salary (10 credits)
    private static final float SUPPLIES_MONTH_MAX = -30f;
    private static final float SUPPLIES_RECOVERY_MAX = 20f;
    private static final float REPAIR_RATE_MAX = 20f;
    private static final float FUEL_USE_MAX = -20f;
    private static final Color COLOR = new Color(231, 203, 24);

    @Override
    public Color getColor() {
        return COLOR;
    }

    private static boolean isAutomated(FleetMemberAPI fm) {
        return Misc.isAutomated(fm) || fm.getMinCrew() <= 0;
    }

    private static boolean isAutomated(ShipVariantAPI var) {
        return Misc.isAutomated(var) || var.getHullSpec().getMinCrew() <= 0;
    }

    @Override
    public boolean canApply(FleetMemberAPI fm) {
        if (isAutomated(fm)) {
            return false;
        }
        return super.canApply(fm);
    }

    @Override
    public boolean canApply(ShipVariantAPI var) {
        if (isAutomated(var)) {
            return false;
        }
        return super.canApply(var);
    }

    public float getIncreasedSalaryForMember(FleetMemberAPI fm, ShipModifications es) {
        float actualCrew = fm.getMinCrew();

        float level = es.getUpgrade(this);
        float maxLevel = this.getMaxLevel(fm);
        float salary = (float) Math.ceil(level / maxLevel * COST_PER_CREW_MAX * actualCrew);

        return salary;
    }

    private static boolean doesEconomyHaveListener() {
        return Global.getSector().getListenerManager().hasListenerOfClass(CommissionedSalaryListener.class);
    }

    @Override
    public void applyUpgradeToStats(FleetMemberAPI fm, MutableShipStatsAPI stats, int level, int maxLevel) {
        if (isAutomated(fm)) {
            StatUtils.setStatMult(stats.getSuppliesPerMonth(), this.getBuffId(), level, 2, maxLevel);
        } else {
            StatUtils.setStatMult(stats.getSuppliesPerMonth(), this.getBuffId(), level, SUPPLIES_MONTH_MAX, maxLevel);
        }

        StatUtils.setStatMult(stats.getSuppliesToRecover(), this.getBuffId(), level, SUPPLIES_RECOVERY_MAX, maxLevel);
        StatUtils.setStatPercent(stats.getRepairRatePercentPerDay(), this.getBuffId(), level, REPAIR_RATE_MAX, maxLevel);
        StatUtils.setStatMult(stats.getFuelUseMod(), this.getBuffId(), level, FUEL_USE_MAX, maxLevel);
    }

    @Override
    public void advanceInCampaign(FleetMemberAPI fm, int level, int maxLevel) {
        if(!doesEconomyHaveListener()) {
            Global.getSector().getListenerManager().addListener(new CommissionedSalaryListener());
        }
    }

    @Override
    public void printStatInfoToTooltip(TooltipMakerAPI tooltip, FleetMemberAPI fm, ShipModifications mods) {
        if (mods.getUpgrade(this) >= this.getMaxLevel(fm)) {
            StringUtils.getTranslation(this.getKey(), "crewSalaryFinal")
                    .format("salaryIncrease", COST_PER_CREW_MAX / this.getMaxLevel(fm) * mods.getUpgrade(this))
                    .addToTooltip(tooltip);
        } else {
            StringUtils.getTranslation(this.getKey(), "crewSalaryShop")
                    .format("salaryIncrease", COST_PER_CREW_MAX / this.getMaxLevel(fm) * mods.getUpgrade(this))
                    .format("perLevel", COST_PER_CREW_MAX / this.getMaxLevel(fm))
                    .format("finalValue", COST_PER_CREW_MAX)
                    .addToTooltip(tooltip);
        }

        this.addBenefitToShopTooltip(tooltip, "hullRepair", fm, mods, REPAIR_RATE_MAX);
        this.addBenefitToShopTooltipMult(tooltip, "fuelConsumptionShop", fm, mods, FUEL_USE_MAX);
        this.addBenefitToShopTooltipMult(tooltip, "suppliesToRecover", fm, mods, SUPPLIES_RECOVERY_MAX);

        if (isAutomated(fm)) {
            StringUtils.getTranslation(this.getKey(), "supplyConsumptionShopAutomated")
                    .format("percent", 200)
                    .addToTooltip(tooltip);
        } else {
            this.addBenefitToShopTooltipMult(tooltip, "supplyConsumptionShop", fm, mods, SUPPLIES_MONTH_MAX);
        }
    }

    @Override
    public void modifyToolTip(TooltipMakerAPI tooltip, FleetMemberAPI fm, ShipModifications systems, boolean expand) {
        int level = systems.getUpgrade(this);

        if (expand) {
            tooltip.addPara(this.getName() + " (%s):", 5, this.getColor(), String.valueOf(level));

            int salary = 10 + COST_PER_CREW_MAX * level / getMaxLevel(fm);
            float totalCostPerMonth = getIncreasedSalaryForMember(fm, systems);
            StringUtils.getTranslation(this.getKey(), "crewSalary")
                    .format("salaryIncrease", salary)
                    .format("finalValue", Math.round(totalCostPerMonth))
                    .addToTooltip(tooltip, 2f);

            float fuelUseMult = fm.getStats().getSuppliesToRecover().getMultStatMod(this.getBuffId()).getValue();
            float savedFuel = fm.getHullSpec().getFuelPerLY() * fuelUseMult;
            float fuelCost = Global.getSector().getEconomy().getCommoditySpec(Commodities.FUEL).getBasePrice();
            StringUtils.getTranslation(this.getKey(), "fuelConsumption")
                    .format("percent", (1f - fuelUseMult) * -100f)
                    .format("finalValue", fm.getHullSpec().getFuelPerLY() * fuelUseMult)
                    .format("creditsSavedPerMonth", savedFuel * fuelCost)
                    .addToTooltip(tooltip, 2f);

            float supplyUseMult = fm.getStats().getSuppliesPerMonth().getMultStatMod(this.getBuffId()).getValue();
            float savedSupply = fm.getHullSpec().getSuppliesPerMonth() * supplyUseMult;
            float supplyCost = Global.getSector().getEconomy().getCommoditySpec(Commodities.SUPPLIES).getBasePrice();

            if (isAutomated(fm)) {
                StringUtils.getTranslation(this.getKey(), "supplyConsumptionAutomated")
                        .format("percent", (supplyUseMult - 1) * 100f)
                        .format("finalValue", savedSupply)
                        .format("creditsSavedPerMonth", savedSupply * supplyCost)
                        .addToTooltip(tooltip, 2f);
            } else {
                StringUtils.getTranslation(this.getKey(), "supplyConsumption")
                        .format("percent", (1f - supplyUseMult) * -100f)
                        .format("finalValue", savedSupply)
                        .format("creditsSavedPerMonth", savedSupply * supplyCost)
                        .addToTooltip(tooltip, 2f);
            }

            this.addBenefitToTooltipMult(tooltip,
                    "suppliesToRecover",
                    fm.getStats().getSuppliesToRecover().getMultStatMod(this.getBuffId()).getValue(),
                    fm.getHullSpec().getSuppliesToRecover());

            this.addBenefitToTooltip(tooltip,
                    "hullRepair",
                    fm.getStats().getRepairRatePercentPerDay().getPercentStatMod(this.getBuffId()).getValue(),
                    fm.getStats().getRepairRatePercentPerDay().getBaseValue());

        } else {
            tooltip.addPara(this.getName() + " (%s)", 5, this.getColor(), String.valueOf(level));
        }
    }

    public class CommissionedSalaryListener implements EconomyTickListener, TooltipMakerAPI.TooltipCreator {
        public void reportEconomyTick(int iterIndex) {
            int lastIterInMonth = (int) Global.getSettings().getFloat("economyIterPerMonth") - 1;
            if (iterIndex != lastIterInMonth) return;

            //all upgrades removed
            float salaryCommission = getSalaryCommission();
            if (salaryCommission <= 0f) {
                Global.getSector().getListenerManager().removeListener(this);
                return;
            }

            if(IronShellIntegration.isEnabled()) {
                IronShellIntegration.setSalaryTax(salaryCommission);
            }

            MonthlyReport report = SharedData.getData().getCurrentReport();
            MonthlyReport.FDNode fleetNode = report.getNode(MonthlyReport.FLEET);

            MonthlyReport.FDNode commissionedCrewsNode = report.getNode(fleetNode, "ET_CC_stipend");
            commissionedCrewsNode.upkeep = salaryCommission;
            commissionedCrewsNode.name = "Salaries for Commissioned Crews";
            commissionedCrewsNode.icon = Global.getSettings().getSpriteName("income_report", "crew");
            commissionedCrewsNode.tooltipCreator = this;
        }

        public void reportEconomyMonthEnd() {
        }

        private float getSalaryCommission() {
            CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
            if(fleet == null) {
                return 0;
            }

            int increasedSalary = 0;
            for(FleetMemberAPI fm : fleet.getMembersWithFightersCopy()) {
                if(ETModPlugin.hasData(fm.getId())) {

                    ShipModifications mods = ShipModFactory.getForFleetMember(fm);
                    if(mods.getUpgrade(CommissionedCrews.this.getKey()) > 0) {
                        increasedSalary += CommissionedCrews.getInstance().getIncreasedSalaryForMember(fm, mods);
                    }
                }
            }

            return increasedSalary;
        }


        public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
            tooltip.addPara("Monthly cost of commissioned crews: %s credits",
                    0f, Misc.getHighlightColor(), Misc.getDGSCredits(getSalaryCommission()));
        }

        public float getTooltipWidth(Object tooltipParam) {
            return 450;
        }

        public boolean isTooltipExpandable(Object tooltipParam) {
            return false;
        }
    }

    public static CommissionedCrews getInstance() {
        return (CommissionedCrews) Upgrade.get("CommissionedCrews");
    }
}
