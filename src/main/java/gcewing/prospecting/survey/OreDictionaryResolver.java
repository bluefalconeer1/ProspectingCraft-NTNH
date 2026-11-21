package gcewing.prospecting.survey;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

/** Resolves item stacks and registered block ores to the canonical material names used in survey storage. */
public final class OreDictionaryResolver {

    /**
     * Ore blocks that carry no "ore*" ore-dictionary entry, keyed by block registry name. Pam's HarvestCraft salt
     * ore is only reachable this way: the mod registers its salt item under dustSalt/itemSalt but leaves the ore
     * block untagged.
     */
    private static final Map<String, String> UNTAGGED_ORE_BLOCKS = createUntaggedOreBlocks();

    private OreDictionaryResolver() {}

    public static Set<String> getCanonicalOreNames(ItemStack stack) {
        int[] oreIds = OreDictionary.getOreIDs(stack);
        List<String> oreDictionaryNames = new ArrayList<String>(oreIds.length);
        for (int oreId : oreIds) oreDictionaryNames.add(OreDictionary.getOreName(oreId));
        Set<String> canonicalNames = OreNameNormalizer.normalizeAll(oreDictionaryNames);
        String untaggedName = getUntaggedOreName(stack);
        if (untaggedName == null) return canonicalNames;
        Set<String> result = new LinkedHashSet<String>(canonicalNames);
        result.add(untaggedName);
        return result;
    }

    public static Set<String> getRegisteredCanonicalBlockOreNames() {
        Set<String> canonicalNames = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        for (String oreDictionaryName : OreDictionary.getOreNames()) {
            String canonicalName = OreNameNormalizer.normalize(oreDictionaryName);
            if (canonicalName != null && containsBlock(OreDictionary.getOres(oreDictionaryName)))
                canonicalNames.add(canonicalName);
        }
        for (Map.Entry<String, String> entry : UNTAGGED_ORE_BLOCKS.entrySet())
            if (Block.getBlockFromName(entry.getKey()) != null) canonicalNames.add(entry.getValue());
        return Collections.unmodifiableSet(canonicalNames);
    }

    private static String getUntaggedOreName(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return null;
        Block block = Block.getBlockFromItem(stack.getItem());
        if (block == null || block == Blocks.air) return null;
        String registryName = Block.blockRegistry.getNameForObject(block);
        return registryName == null ? null : UNTAGGED_ORE_BLOCKS.get(registryName);
    }

    private static Map<String, String> createUntaggedOreBlocks() {
        Map<String, String> blocks = new LinkedHashMap<String, String>();
        blocks.put("harvestcraft:salt", "Salt");
        return Collections.unmodifiableMap(blocks);
    }

    private static boolean containsBlock(Iterable<ItemStack> stacks) {
        for (ItemStack stack : stacks)
            if (stack != null && stack.getItem() != null && Block.getBlockFromItem(stack.getItem()) != Blocks.air)
                return true;
        return false;
    }
}
