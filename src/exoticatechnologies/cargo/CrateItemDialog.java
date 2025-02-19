package exoticatechnologies.cargo;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import exoticatechnologies.modifications.exotics.ExoticSpecialItemPlugin;
import exoticatechnologies.modifications.exotics.GenericExoticItemPlugin;
import exoticatechnologies.modifications.upgrades.UpgradeSpecialItemPlugin;
import exoticatechnologies.util.StringUtils;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@RequiredArgsConstructor
public class CrateItemDialog implements InteractionDialogPlugin {
    private final CargoAPI playerCargo;
    private final CrateItemPlugin cratePlugin;
    private InteractionDialogAPI dialog;
    private boolean loadedChestCargo = false;

    @Override
    public void init(InteractionDialogAPI dialog) {
        this.dialog = dialog;
        test();
    }

    public void test() {
        final CargoAPI availableCargo = Global.getFactory().createCargo(false);


        for (CargoStackAPI stack : this.playerCargo.getStacksCopy()) {
            SpecialItemPlugin plugin = stack.getPlugin();
            if (plugin instanceof GenericExoticItemPlugin || plugin instanceof UpgradeSpecialItemPlugin) {
                availableCargo.addSpecial(stack.getSpecialDataIfSpecial(), stack.getSize());
                playerCargo.removeStack(stack);
            }
        }

        availableCargo.sort();
        this.dialog.showCargoPickerDialog("Crate", StringUtils.getString("CrateText", "MoveOption"), StringUtils.getString("CrateText", "CancelOption"), true, 240.0F, availableCargo, new CargoPickerListener() {
            public void pickedCargo(CargoAPI selectedCargo) {
                selectedCargo.removeAll(cratePlugin.getCargo());
                cratePlugin.getCargo().addAll(selectedCargo);
                cratePlugin.getCargo().removeAll(availableCargo);

                playerCargo.removeAll(selectedCargo);

                for (CargoStackAPI stack : availableCargo.getStacksCopy()) {
                    if (stack.getPlugin() instanceof UpgradeSpecialItemPlugin) {
                        ((UpgradeSpecialItemPlugin) stack.getPlugin()).setIgnoreCrate(true);
                    } else if (stack.getPlugin() instanceof ExoticSpecialItemPlugin) {
                        ((ExoticSpecialItemPlugin) stack.getPlugin()).setIgnoreCrate(true);
                    }
                }

                playerCargo.addAll(availableCargo);

                for (CargoStackAPI stack : cratePlugin.getCargo().getStacksCopy()) {
                    if (stack.isNull() || stack.getSize() == 0) {
                        cratePlugin.getCargo().removeStack(stack);
                    }
                }

                closeDialog();
            }

            public void cancelledCargoSelection() {
                playerCargo.addAll(availableCargo);
                closeDialog();
            }

            public void recreateTextPanel(TooltipMakerAPI panel, CargoAPI cargo, CargoStackAPI pickedUp, boolean pickedUpFromSource, CargoAPI combined) {
                float opad = 10.0F;
                if (!loadedChestCargo) {
                    loadedChestCargo = true;
                    cargo.addAll(cratePlugin.getCargo());
                }
                combined.removeAll(cratePlugin.getCargo());

                panel.setParaOrbitronLarge();
                panel.addPara(StringUtils.getString("CrateText", "TitleText"), Global.getSector().getPlayerFaction().getBaseUIColor(), opad);
                panel.setParaFontDefault();

                StringUtils.getTranslation("CrateText", "SideText")
                                .addToTooltip(panel);
                StringUtils.getTranslation("CrateText", "SideText2")
                        .addToTooltip(panel, 1);
            }

            private void closeDialog() {
                Global.getSector().addTransientScript(new CrateHideScript());
                dialog.dismiss();
            }
        });
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {

    }

    @Override
    public void optionMousedOver(String optionText, Object optionData) {

    }

    @Override
    public void advance(float amount) {

    }

    @Override
    public void backFromEngagement(EngagementResultAPI battleResult) {

    }

    @Override
    public Object getContext() {
        return null;
    }

    @Override
    public Map<String, MemoryAPI> getMemoryMap() {
        return null;
    }
}
