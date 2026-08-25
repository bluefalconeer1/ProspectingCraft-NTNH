package gcewing.prospecting.survey;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class OreNameNormalizer {

    private static final String ORE_PREFIX = "ore";
    private static final Map<String, String> MATERIAL_ALIASES = createMaterialAliases();

    private OreNameNormalizer() {}

    public static String normalize(String oreDictionaryName) {
        if (oreDictionaryName == null || !oreDictionaryName.startsWith(ORE_PREFIX)) return null;
        String materialName = oreDictionaryName.substring(ORE_PREFIX.length());
        if (materialName.isEmpty() || !Character.isUpperCase(materialName.charAt(0))) return null;
        String alias = MATERIAL_ALIASES.get(materialName);
        return alias == null ? materialName : alias;
    }

    public static Set<String> normalizeAll(Iterable<String> oreDictionaryNames) {
        Set<String> result = new LinkedHashSet<String>();
        for (String oreDictionaryName : oreDictionaryNames) {
            String normalizedName = normalize(oreDictionaryName);
            if (normalizedName != null) result.add(normalizedName);
        }
        return result;
    }

    private static Map<String, String> createMaterialAliases() {
        Map<String, String> aliases = new HashMap<String, String>();
        aliases.put("NickelPure", "Nickel");
        return Collections.unmodifiableMap(aliases);
    }
}
