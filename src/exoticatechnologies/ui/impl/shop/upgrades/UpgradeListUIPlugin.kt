package exoticatechnologies.ui.impl.shop.upgrades

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import exoticatechnologies.modifications.ShipModifications
import exoticatechnologies.modifications.upgrades.Upgrade
import exoticatechnologies.ui.lists.ListItemUIPanelPlugin
import exoticatechnologies.ui.lists.ListUIPanelPlugin
import exoticatechnologies.util.StringUtils
import java.awt.Color

class UpgradeListUIPlugin(parentPanel: CustomPanelAPI,
                          var member: FleetMemberAPI,
                          var mods: ShipModifications,
                          var market: MarketAPI?
): ListUIPanelPlugin<Upgrade>(parentPanel) {
    override val listHeader = StringUtils.getTranslation("UpgradesDialog", "OpenUpgradeOptions").toString()
    override var bgColor: Color = Color(255, 70, 255, 0)

    override fun createPanelForItem(tooltip: TooltipMakerAPI, item: Upgrade): ListItemUIPanelPlugin<Upgrade> {
        val rowPlugin = UpgradeListItemUIPlugin(item, member, mods, this)
        rowPlugin.panelWidth = panelWidth
        rowPlugin.panelHeight = rowHeight
        rowPlugin.layoutPanel(tooltip)
        return rowPlugin
    }

    override fun shouldMakePanelForItem(item: Upgrade): Boolean {
        if (mods.hasUpgrade(item)) {
            return true
        }

        return item.shouldShow(member, mods, market!!) && item.canApply(member)
    }
}