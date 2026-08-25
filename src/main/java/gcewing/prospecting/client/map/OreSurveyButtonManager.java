package gcewing.prospecting.client.map;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import com.gtnewhorizons.navigator.api.model.SupportedMods;
import com.gtnewhorizons.navigator.api.model.buttons.ButtonManager;

public class OreSurveyButtonManager extends ButtonManager {

    public static final OreSurveyButtonManager INSTANCE = new OreSurveyButtonManager();

    private static final ResourceLocation ICON = new ResourceLocation(
        "prospectingcraft",
        "textures/items/seismic_survey.png");

    private OreSurveyButtonManager() {}

    @Override
    public ResourceLocation getIcon(SupportedMods mod, String theme) {
        return ICON;
    }

    @Override
    public String getButtonText() {
        return StatCollector.translateToLocal("prospectingcraft.map.ore_survey");
    }
}
