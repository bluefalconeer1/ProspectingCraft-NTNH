package gcewing.prospecting.survey;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class OreNameNormalizer {

    private static final String ORE_PREFIX = "ore";
    private static final String NETHER_PREFIX = "Nether";
    private static final Map<String, String> MATERIAL_ALIASES = createMaterialAliases();
    private static final Set<String> EXCLUDED_MATERIAL_NAMES = Collections.singleton("Volcanic");

    private OreNameNormalizer() {}

    public static String normalize(String oreDictionaryName) {
        if (oreDictionaryName == null || !oreDictionaryName.startsWith(ORE_PREFIX)) return null;
        String materialName = oreDictionaryName.substring(ORE_PREFIX.length());
        if (materialName.isEmpty() || !Character.isUpperCase(materialName.charAt(0))) return null;
        return normalizeCanonicalName(materialName);
    }

    public static String normalizeCanonicalName(String materialName) {
        if (materialName == null) return null;
        if (materialName.startsWith(NETHER_PREFIX) && materialName.length() > NETHER_PREFIX.length()
            && Character.isUpperCase(materialName.charAt(NETHER_PREFIX.length())))
            materialName = materialName.substring(NETHER_PREFIX.length());
        String alias = MATERIAL_ALIASES.get(materialName);
        String canonicalName = alias == null ? materialName : alias;
        return EXCLUDED_MATERIAL_NAMES.contains(canonicalName) ? null : canonicalName;
    }

    public static Set<String> normalizeAll(Iterable<String> oreDictionaryNames) {
        Set<String> result = new LinkedHashSet<String>();
        for (String oreDictionaryName : oreDictionaryNames) {
            String normalizedName = normalize(oreDictionaryName);
            if (normalizedName != null) result.add(normalizedName);
        }
        return result;
    }

    static boolean isLegacyHbmThoriumStorageName(String materialName) {
        return "Thorium".equals(materialName) || "Thorium232".equals(materialName) || "Th232".equals(materialName);
    }

    private static Map<String, String> createMaterialAliases() {
        Map<String, String> aliases = new HashMap<String, String>();
        aliases.put("NickelPure", "Nickel");
        aliases.put("Th232", "Thorium");
        aliases.put("Thorium232", "Thorium");
        return Collections.unmodifiableMap(aliases);
    }
}
