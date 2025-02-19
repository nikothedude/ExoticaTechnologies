package exoticatechnologies.ui.impl.ships

import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.LabelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import exoticatechnologies.modifications.ShipModifications
import exoticatechnologies.ui.lists.ListItemUIPanelPlugin
import exoticatechnologies.ui.lists.ListUIPanelPlugin
import java.awt.Color

class ShipItemUIPlugin(
    item: FleetMemberAPI, var mods: ShipModifications, private val listPanel: ListUIPanelPlugin<FleetMemberAPI>
) : ListItemUIPanelPlugin<FleetMemberAPI>(item) {
    private val pad = 3f
    private val opad = 10f
    override var panelWidth: Float = 304f
    override var panelHeight: Float = 64f
    override var bgColor: Color = Color(200, 200, 200, 0)
    var lastSpecialValue: Float = -1f
    var specialText: LabelAPI? = null
    var wasHovered: Boolean = false

    fun updateWithNewMods(mods: ShipModifications) {
        this.mods = mods
        val newSpecialValue = mods.value
        setSpecialText(newSpecialValue)
    }

    override fun advance(amount: Float) {
        val newSpecialValue = mods.value
        if (newSpecialValue != lastSpecialValue) {
            setSpecialText(newSpecialValue)
        }
    }

    override fun layoutPanel(tooltip: TooltipMakerAPI): CustomPanelAPI {
        val shipNameColor = item.captain.faction.baseUIColor

        val rowPanel: CustomPanelAPI =
            listPanel.parentPanel.createCustomPanel(panelWidth, panelHeight, this)

        // Ship image with tooltip of the ship class
        val shipImg = rowPanel.createUIElement(iconSize, iconSize, false)
        shipImg.addShipList(1, 1, iconSize, Misc.getBasePlayerColor(), mutableListOf(item), 0f)
        rowPanel.addUIElement(shipImg).inTL(0f, 0f)

        // Ship name, class, bandwidth
        val shipText = rowPanel.createUIElement(panelWidth - iconSize, panelHeight, false)
        shipText.addPara(item.shipName, shipNameColor, 0f)
        shipText.addPara(item.hullSpec.hullNameWithDashClass, 0f)

        val specialValue = mods.value
        specialText = shipText.addPara("", Misc.getTextColor(), 0f)
        setSpecialText(specialValue)

        rowPanel.addUIElement(shipText).rightOfTop(shipImg, pad)

        // done, add row to TooltipMakerAPI
        tooltip.addCustom(rowPanel, opad)

        panel = rowPanel

        return panel!!
    }

    private fun setSpecialText(newSpecialValue: Float) {
        lastSpecialValue = newSpecialValue

        val upgrades: Int = mods.upgradeMap.size
        val exotics: Int = mods.exoticSet.size

        specialText?.let {
            it.text = "$upgrades | $exotics"
            it.setHighlightColors(Misc.getEnergyMountColor(), Misc.getHighlightColor())
            it.setHighlight("$upgrades", "$exotics")
        }
    }

    override fun processInput(events: List<InputEventAPI>) {
        if (bgColor.alpha >= 100) return // selected already

        if (isHovered(events)) {
            if (!wasHovered) {
                wasHovered = true
                setBGColor(alpha = 50)
            }
        } else if (wasHovered) {
            wasHovered = false
            setBGColor(alpha = 0)
        }
    }
}