package gcewing.prospecting.survey;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.storage.MapStorage;

public class PlayerSurveyData extends WorldSavedData {

    private static final String DATA_NAME_PREFIX = "prospectingcraft_surveys_";
    private static final int FORMAT_VERSION = 1;
    private static final int COMPOUND_TAG_ID = 10;

    private final UUID ownerId;
    private final List<SurveyArea> surveyAreas = new ArrayList<SurveyArea>();

    public PlayerSurveyData(String dataName) {
        super(dataName);
        ownerId = parseOwnerId(dataName);
    }

    public static PlayerSurveyData get(World world, UUID ownerId) {
        if (world.isRemote) throw new IllegalArgumentException("Player survey data is server-only");
        String dataName = getDataName(ownerId);
        MapStorage storage = world.perWorldStorage;
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
        for (int i = surveyAreas.size() - 1; i >= 0; i--) {
            SurveyArea area = surveyAreas.get(i);
            if (area.getDimensionId() == dimensionId && area.isSurveyed(x, z)) return area.getColumn(x, z);
        }
        return null;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        int formatVersion = nbt.getInteger("formatVersion");
        if (formatVersion != FORMAT_VERSION)
            throw new IllegalArgumentException("Unsupported player survey data version: " + formatVersion);

        UUID savedOwnerId = UUID.fromString(nbt.getString("ownerId"));
        if (!ownerId.equals(savedOwnerId))
            throw new IllegalArgumentException("Survey data owner does not match its name");

        surveyAreas.clear();
        NBTTagList areas = nbt.getTagList("areas", COMPOUND_TAG_ID);
        for (int i = 0; i < areas.tagCount(); i++) surveyAreas.add(SurveyAreaNbt.read(areas.getCompoundTagAt(i)));
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        nbt.setInteger("formatVersion", FORMAT_VERSION);
        nbt.setString("ownerId", ownerId.toString());
        NBTTagList areas = new NBTTagList();
        for (SurveyArea surveyArea : surveyAreas) areas.appendTag(SurveyAreaNbt.write(surveyArea));
        nbt.setTag("areas", areas);
    }

    private static UUID parseOwnerId(String dataName) {
        if (dataName == null || !dataName.startsWith(DATA_NAME_PREFIX))
            throw new IllegalArgumentException("Invalid player survey data name: " + dataName);
        return UUID.fromString(dataName.substring(DATA_NAME_PREFIX.length()));
    }
}
