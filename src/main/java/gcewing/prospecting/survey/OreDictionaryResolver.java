package gcewing.prospecting.survey;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

/** Resolves item stacks and registered block ores to the canonical material names used in survey storage. */
public final class OreDictionaryResolver {

    private OreDictionaryResolver() {}

    public static Set<String> getCanonicalOreNames(ItemStack stack) {
        int[] oreIds = OreDictionary.getOreIDs(stack);
        List<String> oreDictionaryNames = new ArrayList<String>(oreIds.length);
        for (int oreId : oreIds) oreDictionaryNames.add(OreDictionary.getOreName(oreId));
        return OreNameNormalizer.normalizeAll(oreDictionaryNames);
    }

    public static Set<String> getRegisteredCanonicalBlockOreNames() {
        Set<String> canonicalNames = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        for (String oreDictionaryName : OreDictionary.getOreNames()) {
            String canonicalName = OreNameNormalizer.normalize(oreDictionaryName);
            if (canonicalName != null && containsBlock(OreDictionary.getOres(oreDictionaryName)))
                canonicalNames.add(canonicalName);
        }
        return Collections.unmodifiableSet(canonicalNames);
    }

    private static boolean containsBlock(Iterable<ItemStack> stacks) {
        for (ItemStack stack : stacks)
            if (stack != null && stack.getItem() != null && Block.getBlockFromItem(stack.getItem()) != Blocks.air)
                return true;
        return false;
    }
}
