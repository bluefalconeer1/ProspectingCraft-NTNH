package gcewing.prospecting;

import net.minecraft.item.ItemStack;

import codechicken.nei.api.API;
import codechicken.nei.api.IConfigureNEI;

public class NEIProspectingCraftConfig implements IConfigureNEI {

    @Override
    public void loadConfig() {
        API.hideItem(new ItemStack(ProspectingCraft.blockGSAKit));
        API.hideItem(new ItemStack(ProspectingCraft.itemSeismicSurvey));
        API.hideItem(new ItemStack(ProspectingCraft.itemSowsEar));
        API.hideItem(new ItemStack(ProspectingCraft.itemGeologicalSample));
        API.hideItem(new ItemStack(ProspectingCraft.itemProspectingPick));
        API.hideItem(new ItemStack(ProspectingCraft.itemLitmusPaper));
        API.hideItem(new ItemStack(ProspectingCraft.itemRedtoothTransmitter));
        API.hideItem(new ItemStack(ProspectingCraft.itemRedtoothReceiver));
        API.hideItem(new ItemStack(ProspectingCraft.itemTestTube));
        API.hideItem(new ItemStack(ProspectingCraft.itemEyeglass));
        API.hideItem(new ItemStack(ProspectingCraft.itemMagnetometer));
        API.hideItem(new ItemStack(ProspectingCraft.itemNaquadahDetector));
    }

    @Override
    public String getName() {
        return Info.modName;
    }

    @Override
    public String getVersion() {
        return Info.versionNumber;
    }
}
