package gcewing.prospecting.survey;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

public final class OreDictionaryResolver {

    private OreDictionaryResolver() {}

    public static Set<String> getCanonicalOreNames(ItemStack stack) {
        int[] oreIds = OreDictionary.getOreIDs(stack);
        List<String> oreDictionaryNames = new ArrayList<String>(oreIds.length);
        for (int oreId : oreIds) oreDictionaryNames.add(OreDictionary.getOreName(oreId));
        return OreNameNormalizer.normalizeAll(oreDictionaryNames);
    }
}
