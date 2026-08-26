package gcewing.prospecting.survey;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.storage.MapStorage;

public class PlayerSurveyData extends WorldSavedData {

    private static final String DATA_NAME_PREFIX = "prospectingcraft_surveys_";
    private static final int FORMAT_VERSION = 1;
    private static final int STRING_TAG_ID = 8;
    private static final int COMPOUND_TAG_ID = 10;

    private final UUID ownerId;
    private final List<SurveyArea> surveyAreas = new ArrayList<SurveyArea>();
    private SurveyOreSelection oreSelection = SurveyOreSelection.showAll();

    public PlayerSurveyData(String dataName) {
        super(dataName);
        ownerId = parseOwnerId(dataName);
    }

    public static PlayerSurveyData get(World world, UUID ownerId) {
        if (world.isRemote) throw new IllegalArgumentException("Player survey data is server-only");
        String dataName = getDataName(ownerId);
        // SurveyArea records its dimension, so keep one player database shared by every dimension.
        MapStorage storage = world.mapStorage;
        PlayerSurveyData data = (PlayerSurveyData) storage.loadData(PlayerSurveyData.class, dataName);
        if (data == null) {
            data = new PlayerSurveyData(dataName);
            storage.setData(dataName, data);
        }
        return data;
    }

    public static String getDataName(UUID ownerId) {
        if (ownerId == null) throw new IllegalArgumentException("Survey owner must not be null");
        return DATA_NAME_PREFIX + ownerId.toString();
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public int getSurveyAreaCount() {
        return surveyAreas.size();
    }

    public SurveyOreSelection getOreSelection() {
        return oreSelection;
    }

    public void setOreSelection(SurveyOreSelection oreSelection) {
        if (oreSelection == null) throw new IllegalArgumentException("Survey ore selection must not be null");
        this.oreSelection = oreSelection;
        markDirty();
    }

    public Set<String> getDiscoveredOreNames() {
        Set<String> oreNames = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        for (SurveyArea surveyArea : surveyAreas) for (SurveyColumn column : surveyArea.getOreColumns()
            .values())
            oreNames.addAll(
                column.getOreCounts()
                    .keySet());
        return Collections.unmodifiableSet(oreNames);
    }

    public SurveyOreSelection canonicalizeOreSelection(SurveyOreSelection requestedSelection) {
        return canonicalizeOreSelection(requestedSelection, getDiscoveredOreNames());
    }

    public SurveyOreSelection canonicalizeOreSelection(SurveyOreSelection requestedSelection,
        Iterable<String> selectableOreNames) {
        if (requestedSelection == null) throw new IllegalArgumentException("Survey ore selection must not be null");
        if (selectableOreNames == null) throw new IllegalArgumentException("Selectable ore names must not be null");
        if (requestedSelection.getMode() == SurveyOreSelection.Mode.SHOW_ALL) return SurveyOreSelection.showAll();

        List<String> resolvedNames = new ArrayList<String>();
        for (String requestedName : requestedSelection.getSelectedOreNames()) {
            String resolvedName = findIgnoringCase(selectableOreNames, requestedName);
            if (resolvedName == null)
                throw new IllegalArgumentException("Ore is not a selectable registered block ore: " + requestedName);
            resolvedNames.add(resolvedName);
        }
        return SurveyOreSelection.custom(resolvedNames);
    }

    public void addSurveyArea(SurveyArea surveyArea) {
        if (surveyArea == null) throw new IllegalArgumentException("Survey area must not be null");
        if (getSurveyArea(surveyArea.getSurveyId()) != null)
            throw new IllegalArgumentException("Survey ID is already present: " + surveyArea.getSurveyId());
        surveyAreas.add(surveyArea);
        markDirty();
    }

    public SurveyArea getSurveyArea(UUID surveyId) {
        if (surveyId == null) return null;
        for (SurveyArea surveyArea : surveyAreas) if (surveyId.equals(surveyArea.getSurveyId())) return surveyArea;
        return null;
    }

    public boolean isSurveyed(int dimensionId, int x, int z) {
        return getColumn(dimensionId, x, z) != null;
    }

    public SurveyColumn getColumn(int dimensionId, int x, int z) {
        SurveyArea area = getSurveyAreaAt(dimensionId, x, z);
        return area == null ? null : area.getColumn(x, z);
    }

    public SurveyArea getSurveyAreaAt(int dimensionId, int x, int z) {
        SurveyArea bestArea = null;
        for (int i = surveyAreas.size() - 1; i >= 0; i--) {
            SurveyArea area = surveyAreas.get(i);
            if (area.getDimensionId() == dimensionId && area.isSurveyed(x, z)
                && (bestArea == null || area.getTier()
                    .getStorageId()
                    > bestArea.getTier()
                        .getStorageId()))
                bestArea = area;
        }
        return bestArea;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        int formatVersion = nbt.getInteger("formatVersion");
        if (formatVersion != FORMAT_VERSION)
            throw new IllegalArgumentException("Unsupported player survey data version: " + formatVersion);

        UUID savedOwnerId = UUID.fromString(nbt.getString("ownerId"));
        if (!ownerId.equals(savedOwnerId))
            throw new IllegalArgumentException("Survey data owner does not match its name");

        oreSelection = readOreSelection(nbt);
        surveyAreas.clear();
        NBTTagList areas = nbt.getTagList("areas", COMPOUND_TAG_ID);
        for (int i = 0; i < areas.tagCount(); i++) surveyAreas.add(SurveyAreaNbt.read(areas.getCompoundTagAt(i)));
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        nbt.setInteger("formatVersion", FORMAT_VERSION);
        nbt.setString("ownerId", ownerId.toString());
        writeOreSelection(nbt, oreSelection);
        NBTTagList areas = new NBTTagList();
        for (SurveyArea surveyArea : surveyAreas) areas.appendTag(SurveyAreaNbt.write(surveyArea));
        nbt.setTag("areas", areas);
    }

    private static SurveyOreSelection readOreSelection(NBTTagCompound nbt) {
        if (!nbt.getBoolean("customOreSelection")) return SurveyOreSelection.showAll();
        NBTTagList selectedOres = nbt.getTagList("selectedOres", STRING_TAG_ID);
        List<String> selectedOreNames = new ArrayList<String>(selectedOres.tagCount());
        for (int i = 0; i < selectedOres.tagCount(); i++) selectedOreNames.add(selectedOres.getStringTagAt(i));
        return SurveyOreSelection.custom(selectedOreNames);
    }

    private static void writeOreSelection(NBTTagCompound nbt, SurveyOreSelection selection) {
        boolean customSelection = selection.getMode() == SurveyOreSelection.Mode.CUSTOM;
        nbt.setBoolean("customOreSelection", customSelection);
        if (!customSelection) return;
        NBTTagList selectedOres = new NBTTagList();
        for (String oreName : selection.getSelectedOreNames()) selectedOres.appendTag(new NBTTagString(oreName));
        nbt.setTag("selectedOres", selectedOres);
    }

    private static String findIgnoringCase(Iterable<String> names, String requestedName) {
        for (String name : names) if (name.equalsIgnoreCase(requestedName)) return name;
        return null;
    }

    private static UUID parseOwnerId(String dataName) {
        if (dataName == null || !dataName.startsWith(DATA_NAME_PREFIX))
            throw new IllegalArgumentException("Invalid player survey data name: " + dataName);
        return UUID.fromString(dataName.substring(DATA_NAME_PREFIX.length()));
    }
}
