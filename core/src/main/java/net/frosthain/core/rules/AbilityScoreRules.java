package net.frosthain.core.rules;

import net.frosthain.core.content.AbilityScore;
import net.frosthain.core.json.ContentParseException;
import net.frosthain.core.json.JsonObjectReader;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Rules for generating starting ability scores: standard array or point buy, plus the background bonus
 * (+2/+1 or +1/+1/+1 on the background's three abilities, as in SRD 5.2.1).
 */
public record AbilityScoreRules(
        List<Integer> standardArray,
        int pointBuyBudget,
        Map<Integer, Integer> pointBuyCosts,
        int maximumScore
) {

    public static AbilityScoreRules parse(JsonObjectReader json) {
        List<Integer> standard = json.intList("standard_array");
        if (standard.size() != AbilityScore.values().length) {
            throw new ContentParseException(json.childPath("standard_array"), "sechs Werte erwartet");
        }
        JsonObjectReader pointBuy = json.object("point_buy");
        int budget = pointBuy.integerInRange("budget", 27, 0, 100);
        Map<Integer, Integer> costs = new TreeMap<>();
        for (Map.Entry<String, Integer> entry : pointBuy.intMap("costs").entrySet()) {
            try {
                costs.put(Integer.parseInt(entry.getKey()), entry.getValue());
            } catch (NumberFormatException e) {
                throw new ContentParseException(pointBuy.childPath("costs"), "Attributwert als Zahl erwartet: " + entry.getKey(), e);
            }
        }
        if (costs.isEmpty()) {
            throw new ContentParseException(pointBuy.childPath("costs"), "Kostentabelle fehlt");
        }
        pointBuy.finish();
        return new AbilityScoreRules(standard, budget, Map.copyOf(costs), json.integerInRange("maximum_score", 20, 10, 30));
    }

    public enum Method { STANDARD_ARRAY, POINT_BUY }

    /** Returns human-readable problems (German), empty if the base scores are valid for the method. */
    public List<String> validateBaseScores(Method method, Map<AbilityScore, Integer> scores) {
        List<String> problems = new ArrayList<>();
        if (scores.size() != AbilityScore.values().length) {
            problems.add("Alle sechs Attribute müssen einen Wert haben.");
            return problems;
        }
        switch (method) {
            case STANDARD_ARRAY -> {
                List<Integer> given = new ArrayList<>(scores.values());
                List<Integer> expected = new ArrayList<>(standardArray);
                given.sort(null);
                expected.sort(null);
                if (!given.equals(expected)) {
                    problems.add("Die Werte müssen genau der Standardreihe " + standardArray + " entsprechen.");
                }
            }
            case POINT_BUY -> {
                int spent = 0;
                for (Map.Entry<AbilityScore, Integer> entry : scores.entrySet()) {
                    Integer cost = pointBuyCosts.get(entry.getValue());
                    if (cost == null) {
                        problems.add(entry.getKey().key() + ": Wert " + entry.getValue() + " ist beim Punktekauf nicht erlaubt.");
                    } else {
                        spent += cost;
                    }
                }
                if (problems.isEmpty() && spent > pointBuyBudget) {
                    problems.add("Punktekauf überschritten: " + spent + " von " + pointBuyBudget + " Punkten.");
                }
            }
        }
        return problems;
    }

    /**
     * Validates the background bonus: either +2/+1 on two different abilities or +1/+1/+1,
     * always restricted to the background's three ability options.
     */
    public List<String> validateBackgroundBonus(List<AbilityScore> allowed, Map<AbilityScore, Integer> bonus) {
        List<String> problems = new ArrayList<>();
        for (AbilityScore ability : bonus.keySet()) {
            if (!allowed.contains(ability)) {
                problems.add(ability.key() + " gehört nicht zu den Attributen dieses Hintergrunds.");
            }
        }
        List<Integer> values = new ArrayList<>(bonus.values());
        values.removeIf(v -> v == 0);
        values.sort(null);
        if (!values.equals(List.of(1, 2)) && !values.equals(List.of(1, 1, 1))) {
            problems.add("Bonus muss +2/+1 oder +1/+1/+1 sein.");
        }
        return problems;
    }

    /** Applies the background bonus and caps every score at {@link #maximumScore()}. */
    public Map<AbilityScore, Integer> applyBonus(Map<AbilityScore, Integer> base, Map<AbilityScore, Integer> bonus) {
        Map<AbilityScore, Integer> result = new EnumMap<>(AbilityScore.class);
        for (AbilityScore ability : AbilityScore.values()) {
            int value = base.getOrDefault(ability, 10) + bonus.getOrDefault(ability, 0);
            result.put(ability, Math.min(value, maximumScore));
        }
        return result;
    }
}
