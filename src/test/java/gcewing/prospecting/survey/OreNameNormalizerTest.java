package gcewing.prospecting.survey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public class OreNameNormalizerTest {

    @Test
    public void removesTheOrePrefix() {
        assertEquals("Iron", OreNameNormalizer.normalize("oreIron"));
        assertEquals("Quartz", OreNameNormalizer.normalize("oreNetherQuartz"));
    }

    @Test
    public void groupsNetherVariantsWithTheirBaseMaterial() {
        assertEquals("Uranium", OreNameNormalizer.normalize("oreNetherUranium"));
        assertEquals(
            Collections.singleton("Uranium"),
            OreNameNormalizer.normalizeAll(Arrays.asList("oreUranium", "oreNetherUranium")));
    }

    @Test
    public void mergesHbmNickelAliases() {
        assertEquals("Nickel", OreNameNormalizer.normalize("oreNickel"));
        assertEquals("Nickel", OreNameNormalizer.normalize("oreNickelPure"));
    }

    @Test
    public void countsOnePhysicalBlockOnlyOnceAcrossAliasTags() {
        assertEquals(
            Collections.singleton("Nickel"),
            OreNameNormalizer.normalizeAll(Arrays.asList("oreNickel", "oreNickelPure", "ingotNickel")));
    }

    @Test
    public void mergesHbmThoriumAliasesForTheSameBlock() {
        assertEquals(
            Collections.singleton("Thorium"),
            OreNameNormalizer.normalizeAll(Arrays.asList("oreThorium", "oreThorium232", "oreTh232")));
    }

    @Test
    public void excludesDecorativeVolcanicOreTag() {
        assertNull(OreNameNormalizer.normalize("oreVolcanic"));
        assertNull(OreNameNormalizer.normalizeCanonicalName("Volcanic"));
    }

    @Test
    public void rejectsNonOreDictionaryEntries() {
        assertNull(OreNameNormalizer.normalize(null));
        assertNull(OreNameNormalizer.normalize("ore"));
        assertNull(OreNameNormalizer.normalize("ingotIron"));
        assertNull(OreNameNormalizer.normalize("oreberryIron"));
    }
}
