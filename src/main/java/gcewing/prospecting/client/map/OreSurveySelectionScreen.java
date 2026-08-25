package gcewing.prospecting.client.map;

import java.util.Collections;
import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.StatCollector;

import com.gtnewhorizons.navigator.api.NavigatorApi;
import com.gtnewhorizons.navigator.api.model.SupportedMods;
import com.gtnewhorizons.navigator.api.model.layers.LayerManager;

import gcewing.prospecting.survey.SurveyOreSelectionState;

public final class OreSurveySelectionScreen extends GuiScreen {

    private static final int ALL_BUTTON = 0;
    private static final int NONE_BUTTON = 1;
    private static final int APPLY_BUTTON = 2;
    private static final int CANCEL_BUTTON = 3;

    private final GuiScreen parent;
    private OreSurveySelectionModel model;
    private OreList oreList;
    private GuiButton allButton;
    private GuiButton noneButton;
    private GuiButton applyButton;
    private boolean requestedState;
    private SurveyOreSelectionState acceptedState;

    public OreSurveySelectionScreen(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void initGui() {
        oreList = new OreList();
        int buttonWidth = 72;
        int gap = 4;
        int left = width / 2 - (buttonWidth * 4 + gap * 3) / 2;
        int buttonY = height - 28;
        allButton = new GuiButton(ALL_BUTTON, left, buttonY, buttonWidth, 20, translate("all"));
        noneButton = new GuiButton(NONE_BUTTON, left + buttonWidth + gap, buttonY, buttonWidth, 20, translate("none"));
        applyButton = new GuiButton(
            APPLY_BUTTON,
            left + (buttonWidth + gap) * 2,
            buttonY,
            buttonWidth,
            20,
            translate("apply"));
        buttonList.add(allButton);
        buttonList.add(noneButton);
        buttonList.add(applyButton);
        buttonList.add(
            new GuiButton(
                CANCEL_BUTTON,
                left + (buttonWidth + gap) * 3,
                buttonY,
                buttonWidth,
                20,
                translate("cancel")));
        updateButtonAvailability();
        if (!requestedState) {
            requestedState = true;
            SurveyChunkClientHandler.INSTANCE.requestSelectionState();
        }
    }

    @Override
    public void updateScreen() {
        SurveyOreSelectionState state = SurveyChunkClientHandler.INSTANCE.getSelectionState();
        if (state != null && state != acceptedState) {
            acceptedState = state;
            model = new OreSurveySelectionModel(state);
            updateButtonAvailability();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        oreList.drawScreen(mouseX, mouseY, partialTicks);
        drawCenteredString(fontRendererObj, translate("title"), width / 2, 12, 0xffffff);
        drawCenteredString(fontRendererObj, getStatusText(), width / 2, 30, 0xc0c0c0);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (!button.enabled) return;
        if (button.id == ALL_BUTTON) model.selectAll();
        else if (button.id == NONE_BUTTON) model.selectNone();
        else if (button.id == APPLY_BUTTON) {
            SurveyChunkClientHandler.INSTANCE.updateSelection(model.toSelection());
            returnToParent();
        } else if (button.id == CANCEL_BUTTON) returnToParent();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (keyCode == 1) returnToParent();
        else super.keyTyped(typedChar, keyCode);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private void updateButtonAvailability() {
        boolean loaded = model != null;
        if (allButton != null) allButton.enabled = loaded;
        if (noneButton != null) noneButton.enabled = loaded;
        if (applyButton != null) applyButton.enabled = loaded;
    }

    private String getStatusText() {
        if (model == null) return translate("loading");
        if (model.isShowAll()) return translate("show_all");
        return StatCollector.translateToLocalFormatted(
            "prospectingcraft.map.ore_selection.selected",
            model.getSelectedCount(),
            model.getOreNames()
                .size());
    }

    private static String translate(String name) {
        return StatCollector.translateToLocal("prospectingcraft.map.ore_selection." + name);
    }

    private void returnToParent() {
        if (parent != null) for (LayerManager manager : NavigatorApi.getEnabledLayers(SupportedMods.XaeroWorldMap)) {
            manager.onGuiOpened(SupportedMods.XaeroWorldMap);
            manager.forceRefresh();
        }
        mc.displayGuiScreen(parent);
    }

    private final class OreList extends GuiSlot {

        private OreList() {
            super(
                OreSurveySelectionScreen.this.mc,
                OreSurveySelectionScreen.this.width,
                OreSurveySelectionScreen.this.height,
                44,
                OreSurveySelectionScreen.this.height - 36,
                20);
            setShowSelectionBox(false);
        }

        @Override
        protected int getSize() {
            return getOreNames().size();
        }

        @Override
        protected void elementClicked(int index, boolean doubleClick, int mouseX, int mouseY) {
            if (model != null) model.toggle(getOreNames().get(index));
        }

        @Override
        protected boolean isSelected(int index) {
            return false;
        }

        @Override
        protected void drawBackground() {}

        @Override
        protected void drawSlot(int index, int x, int y, int slotHeight, Tessellator tessellator, int mouseX,
            int mouseY) {
            String oreName = getOreNames().get(index);
            boolean selected = model.isSelected(oreName);
            String label = (selected ? "[x] " : "[ ] ") + oreName;
            fontRendererObj.drawStringWithShadow(label, x + 4, y + 5, selected ? 0x80ff80 : 0xa0a0a0);
        }

        private List<String> getOreNames() {
            return model == null ? Collections.<String>emptyList() : model.getOreNames();
        }
    }
}
