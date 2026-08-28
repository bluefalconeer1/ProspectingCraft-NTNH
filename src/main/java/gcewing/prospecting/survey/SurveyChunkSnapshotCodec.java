package gcewing.prospecting.survey;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Versioned wire codec for {@link SurveyChunkSnapshot}.
 *
 * <p>
 * Two masks encode surveyed and ore-bearing states compactly. Ore-bearing payloads contain no raw counts and are
 * bounded to five unique names per column.
 */
public final class SurveyChunkSnapshotCodec {

    private static final int FORMAT_VERSION = 3;
    private static final int LEGACY_FORMAT_VERSION = 2;
    private static final int BITS_PER_MASK_PART = 64;
    private static final int MASK_PART_COUNT = SurveyChunkSnapshot.COLUMN_COUNT / BITS_PER_MASK_PART;
    private static final int MAX_TOP_ORE_NAMES = 5;
    private static final int DIRECT_NAMES = 0;
    private static final int BYTE_DICTIONARY = 1;
    private static final int SHORT_DICTIONARY = 2;

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

        if (!hasAnyBit(oreBearingMask)) return;
        OreNameEncoding encoding = OreNameEncoding.forSnapshot(snapshot);
        output.writeByte(encoding.mode);
        if (encoding.mode != DIRECT_NAMES) encoding.writeDictionary(output);
        for (int index = 0; index < SurveyChunkSnapshot.COLUMN_COUNT; index++) {
            SurveyColumnSnapshot column = getColumn(snapshot, index);
            if (column.getState() == SurveyColumnSnapshot.State.ORE_BEARING) writeOreColumn(output, column, encoding);
        }
    }

    public static SurveyChunkSnapshot read(DataInput input) throws IOException {
        if (input == null) throw new IllegalArgumentException("Input must not be null");
        int formatVersion = input.readUnsignedByte();
        if (formatVersion != FORMAT_VERSION && formatVersion != LEGACY_FORMAT_VERSION)
            throw new IOException("Unsupported survey chunk snapshot version: " + formatVersion);

        SurveyChunkSnapshot snapshot = new SurveyChunkSnapshot(input.readInt(), input.readInt(), input.readInt());
        long[] surveyedMask = readMask(input);
        long[] oreBearingMask = readMask(input);
        for (int i = 0; i < MASK_PART_COUNT; i++) if ((oreBearingMask[i] & ~surveyedMask[i]) != 0)
            throw new IOException("Ore-bearing columns must also be marked surveyed");

        OreNameEncoding encoding = OreNameEncoding.direct();
        if (formatVersion == FORMAT_VERSION && hasAnyBit(oreBearingMask)) encoding = OreNameEncoding.read(input);
        for (int index = 0; index < SurveyChunkSnapshot.COLUMN_COUNT; index++) {
            if (getBit(oreBearingMask, index)) setColumn(snapshot, index, readOreColumn(input, encoding));
            else if (getBit(surveyedMask, index)) setColumn(snapshot, index, SurveyColumnSnapshot.SURVEYED_EMPTY);
        }
        return snapshot;
    }

    private static void writeOreColumn(DataOutput output, SurveyColumnSnapshot column, OreNameEncoding encoding)
        throws IOException {
        int oreAlpha = column.getOreAlpha();
        if (oreAlpha <= 0 || oreAlpha > 255)
            throw new IOException("Ore alpha is outside the supported range: " + oreAlpha);
        List<String> topOreNames = column.getTopOreNames();
        if (topOreNames.isEmpty() || topOreNames.size() > MAX_TOP_ORE_NAMES)
            throw new IOException("Ore-bearing columns require between one and five ore names");

        output.writeByte(oreAlpha);
        output.writeByte(topOreNames.size());
        for (String oreName : topOreNames) encoding.writeName(output, oreName);
    }

    private static SurveyColumnSnapshot readOreColumn(DataInput input, OreNameEncoding encoding) throws IOException {
        int oreAlpha = input.readUnsignedByte();
        int nameCount = input.readUnsignedByte();
        if (oreAlpha <= 0) throw new IOException("Ore alpha must be positive");
        if (nameCount <= 0 || nameCount > MAX_TOP_ORE_NAMES)
            throw new IOException("Invalid top ore name count: " + nameCount);

        List<String> topOreNames = new ArrayList<String>(nameCount);
        for (int i = 0; i < nameCount; i++) {
            String oreName = encoding.readName(input);
            if (oreName.isEmpty()) throw new IOException("Ore name must not be empty");
            if (topOreNames.contains(oreName)) throw new IOException("Ore names must be unique");
            topOreNames.add(oreName);
        }
        return SurveyColumnSnapshot.oreBearing(oreAlpha, topOreNames);
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

    private static boolean hasAnyBit(long[] mask) {
        for (long part : mask) if (part != 0) return true;
        return false;
    }

    private static SurveyColumnSnapshot getColumn(SurveyChunkSnapshot snapshot, int index) {
        return snapshot.getColumn(index % SurveyChunkSnapshot.CHUNK_WIDTH, index / SurveyChunkSnapshot.CHUNK_WIDTH);
    }

    private static void setColumn(SurveyChunkSnapshot snapshot, int index, SurveyColumnSnapshot column) {
        snapshot.setColumn(index % SurveyChunkSnapshot.CHUNK_WIDTH, index / SurveyChunkSnapshot.CHUNK_WIDTH, column);
    }

    private static final class OreNameEncoding {

        private final int mode;
        private final List<String> names;
        private final Map<String, Integer> indexes;

        private OreNameEncoding(int mode, List<String> names) {
            this.mode = mode;
            this.names = names;
            indexes = new HashMap<String, Integer>();
            for (int i = 0; i < names.size(); i++) indexes.put(names.get(i), i);
        }

        private static OreNameEncoding direct() {
            return new OreNameEncoding(DIRECT_NAMES, Collections.<String>emptyList());
        }

        private static OreNameEncoding forSnapshot(SurveyChunkSnapshot snapshot) throws IOException {
            Map<String, Integer> references = new HashMap<String, Integer>();
            long directBytes = 0;
            int referenceCount = 0;
            for (int index = 0; index < SurveyChunkSnapshot.COLUMN_COUNT; index++) {
                SurveyColumnSnapshot column = getColumn(snapshot, index);
                if (column.getState() != SurveyColumnSnapshot.State.ORE_BEARING) continue;
                for (String oreName : column.getTopOreNames()) {
                    directBytes += encodedUtfBytes(oreName);
                    referenceCount++;
                    references.put(oreName, 1);
                }
            }

            List<String> names = new ArrayList<String>(references.keySet());
            Collections.sort(names);
            int mode = names.size() <= 256 ? BYTE_DICTIONARY : SHORT_DICTIONARY;
            long dictionaryBytes = 2L + (long) referenceCount * (mode == BYTE_DICTIONARY ? 1 : 2);
            for (String oreName : names) dictionaryBytes += encodedUtfBytes(oreName);
            return dictionaryBytes < directBytes ? new OreNameEncoding(mode, names) : direct();
        }

        private static OreNameEncoding read(DataInput input) throws IOException {
            int mode = input.readUnsignedByte();
            if (mode == DIRECT_NAMES) return direct();
            if (mode != BYTE_DICTIONARY && mode != SHORT_DICTIONARY)
                throw new IOException("Unknown survey ore-name encoding: " + mode);
            int nameCount = input.readUnsignedShort();
            if (nameCount <= 0 || mode == BYTE_DICTIONARY && nameCount > 256)
                throw new IOException("Invalid survey ore-name dictionary size: " + nameCount);
            List<String> names = new ArrayList<String>(nameCount);
            for (int i = 0; i < nameCount; i++) {
                String oreName = input.readUTF();
                if (oreName.isEmpty() || names.contains(oreName))
                    throw new IOException("Survey ore-name dictionary entries must be nonempty and unique");
                names.add(oreName);
            }
            return new OreNameEncoding(mode, names);
        }

        private void writeDictionary(DataOutput output) throws IOException {
            output.writeShort(names.size());
            for (String oreName : names) output.writeUTF(oreName);
        }

        private void writeName(DataOutput output, String oreName) throws IOException {
            if (mode == DIRECT_NAMES) {
                output.writeUTF(oreName);
                return;
            }
            Integer index = indexes.get(oreName);
            if (index == null) throw new IOException("Ore name is missing from the snapshot dictionary: " + oreName);
            if (mode == BYTE_DICTIONARY) output.writeByte(index);
            else output.writeShort(index);
        }

        private String readName(DataInput input) throws IOException {
            if (mode == DIRECT_NAMES) return input.readUTF();
            int index = mode == BYTE_DICTIONARY ? input.readUnsignedByte() : input.readUnsignedShort();
            if (index >= names.size())
                throw new IOException("Survey ore-name dictionary index is out of range: " + index);
            return names.get(index);
        }

        private static int encodedUtfBytes(String value) throws IOException {
            int length = 0;
            for (int i = 0; i < value.length(); i++) {
                int character = value.charAt(i);
                if (character >= 1 && character <= 127) length++;
                else if (character > 2047) length += 3;
                else length += 2;
            }
            if (length > 65535) throw new IOException("Ore name is too long for the survey protocol");
            return 2 + length;
        }
    }
}
