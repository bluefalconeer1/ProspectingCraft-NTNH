package gcewing.prospecting.survey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

import org.junit.Test;

public class SurveyChunkSnapshotCodecTest {

    private static final UUID OWNER_ID = UUID.fromString("57910713-3ba1-4f00-8000-000000000005");

    @Test
    public void snapshotSurvivesBinaryRoundTrip() throws IOException {
        PlayerSurveyData data = new PlayerSurveyData(PlayerSurveyData.getDataName(OWNER_ID));
        SurveyArea area = new SurveyArea(-2, -16, 32, 16, 16);
        area.markSurveyed(-16, 32);
        area.recordOre(-15, 32, "Nickel", 7);
        area.recordOre(-15, 32, "Copper", 5);
        area.recordOre(-15, 32, "Iron", 4);
        area.recordOre(-15, 32, "Tin", 3);
        area.recordOre(-15, 32, "Gold", 2);
        area.recordOre(-15, 32, "Silver", 1);
        area.recordOre(-1, 47, "Gold", 2);
        data.addSurveyArea(area);

        SurveyChunkSnapshot restored = roundTrip(SurveyChunkSnapshot.from(data, -2, -1, 2));

        assertEquals(-2, restored.getDimensionId());
        assertEquals(-1, restored.getChunkX());
        assertEquals(2, restored.getChunkZ());
        assertSame(SurveyColumnSnapshot.SURVEYED_EMPTY, restored.getColumn(0, 0));
        assertSame(SurveyColumnSnapshot.UNSURVEYED, restored.getColumn(2, 0));

        SurveyColumnSnapshot oreColumn = restored.getColumn(1, 0);
        assertEquals(SurveyColumnSnapshot.State.ORE_BEARING, oreColumn.getState());
        assertEquals("Nickel", oreColumn.getDominantOreName());
        assertEquals(144, oreColumn.getOreAlpha());
        assertEquals(Arrays.asList("Nickel", "Copper", "Iron", "Tin", "Gold"), oreColumn.getTopOreNames());
        assertEquals(
            "Gold",
            restored.getColumn(15, 15)
                .getDominantOreName());
    }

    @Test
    public void emptySnapshotUsesFixedCompactHeaderAndMasks() throws IOException {
        PlayerSurveyData data = new PlayerSurveyData(PlayerSurveyData.getDataName(OWNER_ID));
        byte[] encoded = write(SurveyChunkSnapshot.from(data, 0, 0, 0));

        assertEquals(77, encoded.length);
    }

    @Test
    public void repeatedOreNamesUseCompactPacketDictionary() throws IOException {
        SurveyChunkSnapshot oneName = filledSnapshot(Arrays.asList("Iron"));
        SurveyChunkSnapshot threeNames = filledSnapshot(Arrays.asList("Lignite", "Iron", "Gold"));

        assertEquals(854, write(oneName).length);
        assertEquals(1381, write(threeNames).length);
        assertEquals(
            "Iron",
            roundTrip(oneName).getColumn(15, 15)
                .getDominantOreName());
        assertEquals(
            Arrays.asList("Lignite", "Iron", "Gold"),
            roundTrip(threeNames).getColumn(15, 15)
                .getTopOreNames());
    }

    @Test
    public void oneOffOreNamesStayDirectWhenDictionaryWouldBeLarger() throws IOException {
        SurveyChunkSnapshot snapshot = new SurveyChunkSnapshot(0, 0, 0);
        snapshot.setColumn(0, 0, SurveyColumnSnapshot.oreBearing(72, Arrays.asList("Iron")));
        snapshot.setColumn(1, 0, SurveyColumnSnapshot.oreBearing(72, Arrays.asList("Gold")));

        byte[] encoded = write(snapshot);

        assertEquals(0, encoded[77]);
        assertEquals(
            "Iron",
            roundTrip(snapshot).getColumn(0, 0)
                .getDominantOreName());
        assertEquals(
            "Gold",
            roundTrip(snapshot).getColumn(1, 0)
                .getDominantOreName());
    }

    @Test
    public void readsLegacyVersionTwoOrePayload() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(bytes);
        writeHeaderAndMasks(output, 2, 1, 1);
        output.writeByte(144);
        output.writeByte(1);
        output.writeUTF("Iron");
        output.close();

        SurveyChunkSnapshot snapshot = SurveyChunkSnapshotCodec
            .read(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));

        assertEquals(
            "Iron",
            snapshot.getColumn(0, 0)
                .getDominantOreName());
        assertEquals(
            144,
            snapshot.getColumn(0, 0)
                .getOreAlpha());
    }

    @Test
    public void varyingRoundedCornerOpacityDoesNotIncreaseEncodedSize() throws IOException {
        SurveyChunkSnapshot square = new SurveyChunkSnapshot(0, 0, 0);
        SurveyChunkSnapshot rounded = new SurveyChunkSnapshot(0, 0, 0);
        for (int z = 0; z < SurveyChunkSnapshot.CHUNK_WIDTH; z++)
            for (int x = 0; x < SurveyChunkSnapshot.CHUNK_WIDTH; x++) {
                square.setColumn(x, z, SurveyColumnSnapshot.oreBearing(144, Arrays.asList("Iron")));
                rounded.setColumn(x, z, SurveyColumnSnapshot.oreBearing(36 + (x + z) % 109, Arrays.asList("Iron")));
            }

        assertEquals(write(square).length, write(rounded).length);
    }

    @Test(expected = IOException.class)
    public void rejectsUnknownFormatVersion() throws IOException {
        SurveyChunkSnapshotCodec.read(new DataInputStream(new ByteArrayInputStream(new byte[] { 4 })));
    }

    @Test(expected = IOException.class)
    public void rejectsOreColumnsThatAreNotMarkedSurveyed() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(bytes);
        writeHeaderAndMasks(output, 2, 0, 1);
        output.close();

        SurveyChunkSnapshotCodec.read(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
    }

    private static SurveyChunkSnapshot roundTrip(SurveyChunkSnapshot snapshot) throws IOException {
        return SurveyChunkSnapshotCodec.read(new DataInputStream(new ByteArrayInputStream(write(snapshot))));
    }

    private static byte[] write(SurveyChunkSnapshot snapshot) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(bytes);
        SurveyChunkSnapshotCodec.write(output, snapshot);
        output.close();
        return bytes.toByteArray();
    }

    private static SurveyChunkSnapshot filledSnapshot(java.util.List<String> oreNames) {
        SurveyChunkSnapshot snapshot = new SurveyChunkSnapshot(0, 0, 0);
        for (int z = 0; z < SurveyChunkSnapshot.CHUNK_WIDTH; z++)
            for (int x = 0; x < SurveyChunkSnapshot.CHUNK_WIDTH; x++)
                snapshot.setColumn(x, z, SurveyColumnSnapshot.oreBearing(144, oreNames));
        return snapshot;
    }

    private static void writeHeaderAndMasks(DataOutputStream output, int formatVersion, long surveyedBits,
        long oreBearingBits) throws IOException {
        output.writeByte(formatVersion);
        output.writeInt(0);
        output.writeInt(0);
        output.writeInt(0);
        output.writeLong(surveyedBits);
        for (int i = 1; i < 4; i++) output.writeLong(0);
        output.writeLong(oreBearingBits);
        for (int i = 1; i < 4; i++) output.writeLong(0);
    }
}
