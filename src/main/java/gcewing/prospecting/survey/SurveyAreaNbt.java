package gcewing.prospecting.survey;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

public final class SurveyAreaNbt {

    private static final int FORMAT_VERSION = 4;
    private static final int LEGACY_TIER_FORMAT_VERSION = 3;
    private static final int LEGACY_EXACT_FORMAT_VERSION = 2;
    private static final int COMPOUND_TAG_ID = 10;

    private SurveyAreaNbt() {}

    public static NBTTagCompound write(SurveyArea area) {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setInteger("formatVersion", FORMAT_VERSION);
        nbt.setString(
            "surveyId",
            area.getSurveyId()
                .toString());
        nbt.setInteger(
            "tier",
            area.getTier()
                .getStorageId());
        nbt.setInteger("dimensionId", area.getDimensionId());
        nbt.setInteger("minX", area.getMinX());
        nbt.setInteger("minZ", area.getMinZ());
        nbt.setInteger("width", area.getWidth());
        nbt.setInteger("height", area.getHeight());
        nbt.setByteArray(
            "surveyed",
            area.copySurveyedColumns()
                .toByteArray());

        NBTTagList columns = new NBTTagList();
        List<Integer> columnIndexes = new ArrayList<Integer>(
            area.getOreColumns()
                .keySet());
        Collections.sort(columnIndexes);
        for (int columnIndex : columnIndexes) {
            NBTTagCompound columnNbt = new NBTTagCompound();
            columnNbt.setInteger("index", columnIndex);
            columnNbt.setTag(
                "ores",
                writeOres(
                    area.getOreColumns()
                        .get(columnIndex)));
            columns.appendTag(columnNbt);
        }
        nbt.setTag("columns", columns);
        return nbt;
    }

    public static SurveyArea read(NBTTagCompound nbt) {
        int formatVersion = nbt.getInteger("formatVersion");
        if (formatVersion != FORMAT_VERSION && formatVersion != LEGACY_TIER_FORMAT_VERSION
            && formatVersion != LEGACY_EXACT_FORMAT_VERSION)
            throw new IllegalArgumentException("Unsupported survey data version: " + formatVersion);

        SurveyTier tier = formatVersion == LEGACY_EXACT_FORMAT_VERSION ? SurveyTier.DESH
            : SurveyTier.fromStorageId(nbt.getInteger("tier"));

        SurveyArea area = new SurveyArea(
            UUID.fromString(nbt.getString("surveyId")),
            tier,
            nbt.getInteger("dimensionId"),
            nbt.getInteger("minX"),
            nbt.getInteger("minZ"),
            nbt.getInteger("width"),
            nbt.getInteger("height"));
        int columnCount = area.getWidth() * area.getHeight();

        BitSet surveyedColumns = BitSet.valueOf(nbt.getByteArray("surveyed"));
        for (int index = surveyedColumns.nextSetBit(0); index >= 0; index = surveyedColumns.nextSetBit(index + 1)) {
            checkColumnIndex(index, columnCount);
            area.markSurveyed(getX(area, index), getZ(area, index));
        }

        NBTTagList columns = nbt.getTagList("columns", COMPOUND_TAG_ID);
        for (int i = 0; i < columns.tagCount(); i++) {
            NBTTagCompound columnNbt = columns.getCompoundTagAt(i);
            int columnIndex = columnNbt.getInteger("index");
            checkColumnIndex(columnIndex, columnCount);
            readOres(area, columnIndex, columnNbt.getTagList("ores", COMPOUND_TAG_ID), formatVersion < FORMAT_VERSION);
        }
        return area;
    }

    private static NBTTagList writeOres(SurveyColumn column) {
        NBTTagList ores = new NBTTagList();
        List<String> oreNames = new ArrayList<String>(
            column.getOreCounts()
                .keySet());
        Collections.sort(oreNames);
        for (String oreName : oreNames) {
            NBTTagCompound oreNbt = new NBTTagCompound();
            oreNbt.setString("name", oreName);
            oreNbt.setInteger("count", column.getOreCount(oreName));
            ores.appendTag(oreNbt);
        }
        return ores;
    }

    private static void readOres(SurveyArea area, int columnIndex, NBTTagList ores, boolean migrateLegacyAliases) {
        int x = getX(area, columnIndex);
        int z = getZ(area, columnIndex);
        Map<String, Integer> migratedCounts = new HashMap<String, Integer>();
        for (int i = 0; i < ores.tagCount(); i++) {
            NBTTagCompound oreNbt = ores.getCompoundTagAt(i);
            String oreName = oreNbt.getString("name");
            int oreCount = oreNbt.getInteger("count");
            if (migrateLegacyAliases && OreNameNormalizer.isLegacyHbmThoriumStorageName(oreName)) {
                Integer oldCount = migratedCounts.get("Thorium");
                migratedCounts.put("Thorium", oldCount == null ? oreCount : Math.max(oldCount, oreCount));
            } else area.recordOre(x, z, oreName, oreCount);
        }
        for (Map.Entry<String, Integer> entry : migratedCounts.entrySet())
            area.recordOre(x, z, entry.getKey(), entry.getValue());
    }

    private static int getX(SurveyArea area, int columnIndex) {
        return area.getMinX() + columnIndex % area.getWidth();
    }

    private static int getZ(SurveyArea area, int columnIndex) {
        return area.getMinZ() + columnIndex / area.getWidth();
    }

    private static void checkColumnIndex(int columnIndex, int columnCount) {
        if (columnIndex < 0 || columnIndex >= columnCount)
            throw new IllegalArgumentException("Survey column index is outside the survey area: " + columnIndex);
    }
}
