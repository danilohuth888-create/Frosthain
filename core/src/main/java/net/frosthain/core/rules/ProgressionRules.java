package net.frosthain.core.rules;

import net.frosthain.core.json.ContentParseException;
import net.frosthain.core.json.JsonObjectReader;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Experience curve, talent point budget and multiclass limits.
 *
 * <p>FROSTHAIN uses its own experience points ("Erfahrung", EP) that are completely separate from
 * vanilla enchanting XP, so talents never compete with enchanting.
 *
 * @param experienceThresholds total EP needed to <em>reach</em> level i+1 (index 0 = level 1 = 0 EP)
 */
public record ProgressionRules(
        int maxLevel,
        List<Integer> experienceThresholds,
        int talentPointsPerLevel,
        Map<Integer, Integer> bonusTalentPoints,
        Multiclass multiclass,
        DefeatExperience defeatExperience
) {

    /**
     * @param minimumLevelBeforeMulticlass character level required before taking a second class
     * @param maxClasses                   maximum number of different classes per character
     */
    public record Multiclass(boolean enabled, int maxClasses, int minimumLevelBeforeMulticlass) {
    }

    public static ProgressionRules parse(JsonObjectReader json) {
        int maxLevel = json.integerInRange("max_level", 20, 1, 100);
        List<Integer> thresholds = json.intList("experience_thresholds");
        if (thresholds.size() != maxLevel) {
            throw new ContentParseException(json.childPath("experience_thresholds"),
                    "genau " + maxLevel + " Schwellen erwartet (eine pro Stufe), gefunden: " + thresholds.size());
        }
        if (thresholds.getFirst() != 0) {
            throw new ContentParseException(json.childPath("experience_thresholds"), "Stufe 1 muss bei 0 EP beginnen");
        }
        for (int i = 1; i < thresholds.size(); i++) {
            if (thresholds.get(i) <= thresholds.get(i - 1)) {
                throw new ContentParseException(json.childPath("experience_thresholds") + "[" + i + "]",
                        "Schwellen müssen streng steigen");
            }
        }
        Map<Integer, Integer> bonus = new TreeMap<>();
        for (Map.Entry<String, Integer> entry : json.intMap("bonus_talent_points").entrySet()) {
            int level;
            try {
                level = Integer.parseInt(entry.getKey());
            } catch (NumberFormatException e) {
                throw new ContentParseException(json.childPath("bonus_talent_points"), "Stufe als Zahl erwartet: " + entry.getKey(), e);
            }
            if (level < 1 || level > maxLevel || entry.getValue() < 0) {
                throw new ContentParseException(json.childPath("bonus_talent_points"), "ungültiger Eintrag " + entry);
            }
            bonus.put(level, entry.getValue());
        }
        JsonObjectReader mc = json.object("multiclass");
        Multiclass multiclass = new Multiclass(mc.bool("enabled", true),
                mc.integerInRange("max_classes", 3, 1, 12),
                mc.integerInRange("minimum_level_before_multiclass", 2, 1, maxLevel));
        mc.finish();
        DefeatExperience defeat = DefeatExperience.DEFAULT;
        if (json.has("defeat_experience")) {
            JsonObjectReader d = json.object("defeat_experience");
            double perHealth = d.number("per_health", DefeatExperience.DEFAULT.perHealth());
            if (perHealth < 0 || perHealth > 100) {
                throw new ContentParseException(d.childPath("per_health"), "Wert zwischen 0 und 100 erwartet");
            }
            defeat = new DefeatExperience(perHealth, d.integerInRange("maximum", DefeatExperience.DEFAULT.maximum(), 0, 100_000));
            d.finish();
        }
        return new ProgressionRules(maxLevel, thresholds,
                json.integerInRange("talent_points_per_level", 1, 0, 10), Map.copyOf(bonus), multiclass, defeat);
    }

    /**
     * Experience for defeating a hostile creature: {@code perHealth} × its maximum health, at most {@code maximum}.
     * Passive animals give nothing (no reward for farming).
     */
    public record DefeatExperience(double perHealth, int maximum) {
        public static final DefeatExperience DEFAULT = new DefeatExperience(2.5, 500);
    }

    public int experienceForDefeat(double maxHealth, boolean hostile) {
        if (!hostile || maxHealth <= 0) {
            return 0;
        }
        return (int) Math.min(defeatExperience.maximum(), Math.round(maxHealth * defeatExperience.perHealth()));
    }

    /** Character level for a given amount of total experience (1..maxLevel). */
    public int levelForExperience(long experience) {
        if (experience < 0) {
            throw new IllegalArgumentException("negative Erfahrung: " + experience);
        }
        int level = 1;
        for (int i = 1; i < experienceThresholds.size(); i++) {
            if (experience >= experienceThresholds.get(i)) {
                level = i + 1;
            } else {
                break;
            }
        }
        return level;
    }

    /** EP still missing for the next level, or 0 at max level. */
    public long experienceToNextLevel(long experience) {
        int level = levelForExperience(experience);
        if (level >= maxLevel) {
            return 0;
        }
        return experienceThresholds.get(level) - experience;
    }

    /** Total talent points earned up to and including {@code level}. */
    public int talentPointsAtLevel(int level) {
        int clamped = Math.clamp(level, 1, maxLevel);
        int points = clamped * talentPointsPerLevel;
        for (Map.Entry<Integer, Integer> entry : bonusTalentPoints.entrySet()) {
            if (entry.getKey() <= clamped) {
                points += entry.getValue();
            }
        }
        return points;
    }
}
