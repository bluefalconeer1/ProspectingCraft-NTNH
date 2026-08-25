package gcewing.prospecting.survey;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SurveyColumn {

    private final Map<String, Integer> oreCounts = new HashMap<String, Integer>();

    public void addOre(String oreName, int amount) {
        if (oreName == null || oreName.isEmpty()) throw new IllegalArgumentException("Ore name must not be empty");
        if (amount <= 0) throw new IllegalArgumentException("Ore amount must be positive");
        String canonicalName = OreNameNormalizer.normalizeCanonicalName(oreName);
        if (canonicalName == null) return;
        Integer oldAmount = oreCounts.get(canonicalName);
        oreCounts.put(canonicalName, oldAmount == null ? amount : oldAmount + amount);
    }

    public boolean isEmpty() {
        return oreCounts.isEmpty();
    }

    public int getOreCount(String oreName) {
        Integer amount = oreCounts.get(OreNameNormalizer.normalizeCanonicalName(oreName));
        return amount == null ? 0 : amount;
    }

    public String getDominantOreName() {
        List<String> names = getTopOreNames(1);
        return names.isEmpty() ? null : names.get(0);
    }

    public List<String> getTopOreNames(int limit) {
        return getTopOreNames(limit, SurveyOreSelection.showAll());
    }

    public List<String> getTopOreNames(int limit, SurveyOreSelection selection) {
        if (limit < 0) throw new IllegalArgumentException("Limit must not be negative");
        if (selection == null) throw new IllegalArgumentException("Survey ore selection must not be null");
        List<Map.Entry<String, Integer>> entries = new ArrayList<Map.Entry<String, Integer>>();
        for (Map.Entry<String, Integer> entry : oreCounts.entrySet())
            if (selection.includes(entry.getKey())) entries.add(entry);
        Collections.sort(entries, new Comparator<Map.Entry<String, Integer>>() {

            @Override
            public int compare(Map.Entry<String, Integer> left, Map.Entry<String, Integer> right) {
                int amountComparison = right.getValue()
                    .compareTo(left.getValue());
                if (amountComparison != 0) return amountComparison;
                return left.getKey()
                    .compareTo(right.getKey());
            }
        });

        int resultSize = Math.min(limit, entries.size());
        List<String> result = new ArrayList<String>(resultSize);
        for (int i = 0; i < resultSize; i++) result.add(
            entries.get(i)
                .getKey());
        return result;
    }

    Map<String, Integer> getOreCounts() {
        return Collections.unmodifiableMap(oreCounts);
    }
}
