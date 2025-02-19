package exoticatechnologies.ui.impl.shop

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CargoAPI
import com.fs.starfarer.api.campaign.SpecialItemData
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import exoticatechnologies.modifications.ShipModifications
import exoticatechnologies.modifications.bandwidth.Bandwidth
import exoticatechnologies.modifications.bandwidth.BandwidthUtil
import exoticatechnologies.ui.InteractiveUIPanelPlugin
import exoticatechnologies.util.StringUtils
import exoticatechnologies.util.Utilities
import kotlin.math.absoluteValue

abstract class ResourcesUIPlugin(
    var member: FleetMemberAPI,
    var mods: ShipModifications): InteractiveUIPanelPlugin() {
    abstract var mainPanel: CustomPanelAPI?

    fun displayResourceCosts(resourceCosts: MutableMap<String, Float>): TooltipMakerAPI {
        val tooltip = mainPanel!!.createUIElement(panelWidth, panelHeight, false)
        tooltip.addTitle(StringUtils.getString("UpgradeCosts", "UpgradeCostTitle"))

        resourceCosts.forEach { (id, cost) ->
            if (id == Bandwidth.BANDWIDTH_RESOURCE) {
                addBandwidthString(tooltip, cost)
            } else if (Utilities.isResourceString(id)) {
                addItemStringCost(tooltip, id.substring(1), cost)
            } else if (id == Commodities.CREDITS) {
                addCreditCost(tooltip, cost)
            } else if (id == Utilities.STORY_POINTS) {
                addStoryPointsCost(tooltip, cost)
            } else if (Utilities.isSpecialItemId(id)) {
                addSpecialItemCost(tooltip, id, cost)
            } else {
                addResourceCost(tooltip, id, cost)
            }
        }

        mainPanel!!.addUIElement(tooltip).inTL(0f, 0f)

        return tooltip
    }

    fun addBandwidthString(tooltip: TooltipMakerAPI, bandwidth: Float) {
        val used = mods.usedBandwidth
        if (bandwidth > 0f) {
            var bandwidthString = "+${BandwidthUtil.getFormattedBandwidth(bandwidth.absoluteValue)}"
            if (bandwidth > 0) {
                StringUtils.getTranslation("UpgradeCosts", "BandwidthUsedWithUpgrade")
                    .format("usedBandwidth", BandwidthUtil.getFormattedBandwidth(used))
                    .format("upgradeBandwidth", bandwidthString)
                    .addToTooltip(tooltip)
            } else if (bandwidth < 0) {
                StringUtils.getTranslation("ExoticCosts", "BandwidthGivenByExotic")
                    .format("bandwidth", BandwidthUtil.getFormattedBandwidth(used))
                    .format("exoticBandwidth", bandwidthString)
                    .addToTooltip(tooltip)
            }
        } else {
            StringUtils.getTranslation("UpgradeCosts", "BandwidthUsed")
                .format("usedBandwidth", BandwidthUtil.getFormattedBandwidth(used))
                .addToTooltip(tooltip)
        }
    }

    fun addItemStringCost(tooltip: TooltipMakerAPI, name: String, cost: Float) {
        val quantityText = "0"
        if (cost != 0f) {
            var translationKey = "SpecialItemTextWithCost"
            if (cost < 0) {
                translationKey = "SpecialItemTextWithPay"
            }
            StringUtils.getTranslation("CommonOptions", translationKey)
                .format("name", name)
                .format("amount", quantityText)
                .format("cost", StringUtils.formatCost(cost))
                .addToTooltip(tooltip)
        } else {
            StringUtils.getTranslation("CommonOptions", "ResourceText")
                .format("name", name)
                .format("amount", quantityText)
                .addToTooltip(tooltip)
        }
    }

    fun addCreditCost(tooltip: TooltipMakerAPI, cost: Float) {
        val name: String = Global.getSector().economy.getCommoditySpec(Commodities.CREDITS).name
        val credits: Float = member.fleetData.fleet.cargo.credits.get()

        if (cost > 0) {
            StringUtils.getTranslation("UpgradesDialog", "ResourceTextWithCost")
                .format("name", name)
                .format("amount", credits)
                .format("cost", cost * -1)
                .addToTooltip(tooltip)
        } else if (cost < 0) {
            StringUtils.getTranslation("UpgradesDialog", "ResourceTextWithPay")
                .format("name", name)
                .format("amount", credits)
                .format("cost", cost)
                .addToTooltip(tooltip)
        } else {
            StringUtils.getTranslation("UpgradesDialog", "ResourceText")
                .format("name", name)
                .format("amount", credits)
                .addToTooltip(tooltip)
        }
    }

    fun addStoryPointsCost(tooltip: TooltipMakerAPI, cost: Float) {
        if (cost > 0) {
            StringUtils.getTranslation("CommonOptions", "StoryPointCost")
                .format("storyPoints", StringUtils.formatCost(cost), Misc.getStoryOptionColor())
                .addToTooltip(tooltip)
        }
    }

    fun addSpecialItemCost(tooltip: TooltipMakerAPI, id: String, cost: Float) {
        val specialId = Utilities.getSpecialItemId(id)
        val specialParams = Utilities.getSpecialItemParams(id)

        val stack = Utilities.getSpecialStack(Global.getSector().playerFleet.cargo, specialId, specialParams)
        val name: String
        var quantity = 0f
        if (stack != null) {
            name = stack.displayName
            quantity = stack.size
        } else {
            val cargo = Global.getFactory().createCargo(true)
            val fakeData = SpecialItemData(specialId, specialParams)
            val fakeStack = Global.getFactory().createCargoStack(CargoAPI.CargoItemType.SPECIAL, fakeData, cargo)
            name = fakeStack.displayName
        }

        if (cost != 0f) {
            var translationKey = "SpecialItemTextWithCost"
            if (cost < 0) {
                translationKey = "SpecialItemTextWithPay"
            }
            StringUtils.getTranslation("CommonOptions", translationKey)
                .format("name", name)
                .format("amount", Misc.getWithDGS(quantity))
                .format("cost", StringUtils.formatCost(cost))
                .addToTooltip(tooltip)
        } else {
            var quantityText: String? = "-"
            if (quantity > 0) {
                quantityText = Misc.getWithDGS(quantity)
            }
            StringUtils.getTranslation("CommonOptions", "ResourceText")
                .format("name", name)
                .format("amount", quantityText)
                .addToTooltip(tooltip)
        }
    }

    fun addResourceCost(tooltip: TooltipMakerAPI, id: String, cost: Float) {
        //commodities
        val name = Utilities.getItemName(id)
        val quantity = Global.getSector().playerFleet.cargo.getCommodityQuantity(id)

        if (cost != 0f) {
            StringUtils.getTranslation("CommonOptions", "ResourceTextWithCost")
                .format("name", name)
                .format("amount", Misc.getWithDGS(quantity))
                .format("cost", StringUtils.formatCost(cost))
                .addToTooltip(tooltip)
        } else {
            StringUtils.getTranslation("CommonOptions", "ResourceText")
                .format("name", name)
                .format("amount", Misc.getWithDGS(quantity))
                .addToTooltip(tooltip)
        }
    }
}