package gcewing.prospecting.survey;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class SurveyChunkSnapshotCodec {

    private static final int FORMAT_VERSION = 1;
    private static final int BITS_PER_MASK_PART = 64;
    private static final int MASK_PART_COUNT = SurveyChunkSnapshot.COLUMN_COUNT / BITS_PER_MASK_PART;
    private static final int MAX_UNSIGNED_SHORT = 0xffff;
    private static final int MAX_TOP_ORE_NAMES = 5;

    private SurveyChunkSnapshotCodec() {}

    public static void write(DataOutput output, SurveyChunkSnapshot snapshot) throws IOException {
        if (output == null) throw new IllegalArgumentException("Output must not be null");
        if (snapshot == null) throw new IllegalArgumentException("Survey chunk snapshot must not be null");

        output.writeByte(FORMAT_VERSION);
        output.writeInt(snapshot.getDimensionId());
        output.writeInt(snapshot.getChunkX());
        output.writeInt(snapshot.getChunkZ());

        long[] surveyedMask = new long[MASK_PART_COUNT];
        long[] oreBearingMask = new long[MASK_PART_COUNT];
        for (int index = 0; index < SurveyChunkSnapshot.COLUMN_COUNT; index++) {
            SurveyColumnSnapshot column = getColumn(snapshot, index);
            if (column.getState() != SurveyColumnSnapshot.State.UNSURVEYED) setBit(surveyedMask, index);
            if (column.getState() == SurveyColumnSnapshot.State.ORE_BEARING) setBit(oreBearingMask, index);
        }
        writeMask(output, surveyedMask);
        writeMask(output, oreBearingMask);

        for (int index = 0; index < SurveyChunkSnapshot.COLUMN_COUNT; index++) {
            SurveyColumnSnapshot column = getColumn(snapshot, index);
            if (column.getState() == SurveyColumnSnapshot.State.ORE_BEARING) writeOreColumn(output, column);
        }
    }

    public static SurveyChunkSnapshot read(DataInput input) throws IOException {
        if (input == null) throw new IllegalArgumentException("Input must not be null");
        int formatVersion = input.readUnsignedByte();
        if (formatVersion != FORMAT_VERSION)
            throw new IOException("Unsupported survey chunk snapshot version: " + formatVersion);

        SurveyChunkSnapshot snapshot = new SurveyChunkSnapshot(input.readInt(), input.readInt(), input.readInt());
        long[] surveyedMask = readMask(input);
        long[] oreBearingMask = readMask(input);
        for (int i = 0; i < MASK_PART_COUNT; i++) if ((oreBearingMask[i] & ~surveyedMask[i]) != 0)
            throw new IOException("Ore-bearing columns must also be marked surveyed");

        for (int index = 0; index < SurveyChunkSnapshot.COLUMN_COUNT; index++) {
            if (getBit(oreBearingMask, index)) setColumn(snapshot, index, readOreColumn(input));
            else if (getBit(surveyedMask, index)) setColumn(snapshot, index, SurveyColumnSnapshot.SURVEYED_EMPTY);
        }
        return snapshot;
    }

    private static void writeOreColumn(DataOutput output, SurveyColumnSnapshot column) throws IOException {
        int dominantOreCount = column.getDominantOreCount();
        if (dominantOreCount <= 0 || dominantOreCount > MAX_UNSIGNED_SHORT)
            throw new IOException("Dominant ore count is outside the supported range: " + dominantOreCount);
        List<String> topOreNames = column.getTopOreNames();
        if (topOreNames.isEmpty() || topOreNames.size() > MAX_TOP_ORE_NAMES)
            throw new IOException("Ore-bearing columns require between one and five ore names");

        output.writeShort(dominantOreCount);
        output.writeByte(topOreNames.size());
        for (String oreName : topOreNames) output.writeUTF(oreName);
    }

    private static SurveyColumnSnapshot readOreColumn(DataInput input) throws IOException {
        int dominantOreCount = input.readUnsignedShort();
        int nameCount = input.readUnsignedByte();
        if (dominantOreCount <= 0) throw new IOException("Dominant ore count must be positive");
        if (nameCount <= 0 || nameCount > MAX_TOP_ORE_NAMES)
            throw new IOException("Invalid top ore name count: " + nameCount);

        List<String> topOreNames = new ArrayList<String>(nameCount);
        for (int i = 0; i < nameCount; i++) {
            String oreName = input.readUTF();
            if (oreName.isEmpty()) throw new IOException("Ore name must not be empty");
            if (topOreNames.contains(oreName)) throw new IOException("Ore names must be unique");
            topOreNames.add(oreName);
        }
        return SurveyColumnSnapshot.oreBearing(dominantOreCount, topOreNames);
    }

    private static void writeMask(DataOutput output, long[] mask) throws IOException {
        for (long part : mask) output.writeLong(part);
    }

    private static long[] readMask(DataInput input) throws IOException {
        long[] mask = new long[MASK_PART_COUNT];
        for (int i = 0; i < MASK_PART_COUNT; i++) mask[i] = input.readLong();
        return mask;
    }

    private static void setBit(long[] mask, int index) {
        mask[index / BITS_PER_MASK_PART] |= 1L << (index % BITS_PER_MASK_PART);
    }

    private static boolean getBit(long[] mask, int index) {
        return (mask[index / BITS_PER_MASK_PART] & 1L << (index % BITS_PER_MASK_PART)) != 0;
    }

    private static SurveyColumnSnapshot getColumn(SurveyChunkSnapshot snapshot, int index) {
        return snapshot.getColumn(index % SurveyChunkSnapshot.CHUNK_WIDTH, index / SurveyChunkSnapshot.CHUNK_WIDTH);
    }

    private static void setColumn(SurveyChunkSnapshot snapshot, int index, SurveyColumnSnapshot column) {
        snapshot.setColumn(index % SurveyChunkSnapshot.CHUNK_WIDTH, index / SurveyChunkSnapshot.CHUNK_WIDTH, column);
    }
}
