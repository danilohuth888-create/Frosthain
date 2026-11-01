package net.frosthain.core.character;

import net.frosthain.core.ContentId;
import net.frosthain.core.content.SpeciesDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Visual appearance of a character. Purely cosmetic: it can be changed at any time in the character menu.
 *
 * <p>Rendering strategy (see docs/05-charaktereditor-konzept.md): skin colour, face, hair, beard and markings
 * are composited into a 64x64 player skin texture at runtime; ears, horns and tails are additional model
 * parts attached to the player model; height is a visual scale within species limits.
 *
 * <p>Part ids (hair styles, horns, ...) reference appearance part definitions, which are introduced in
 * Phase 2. Until then only their format and the species allow-lists are validated.
 */
public record Appearance(
        BodyType bodyType,
        double heightScale,
        String skinColor,
        Optional<ContentId> skinPattern,
        ContentId eyeStyle,
        String eyeColor,
        ContentId hairStyle,
        String hairColor,
        Optional<ContentId> beardStyle,
        Map<String, ContentId> features,
        List<Marking> markings
) {

    public enum BodyType { CLASSIC, SLIM }

    /** Scars, tattoos and magical markings. */
    public record Marking(Kind kind, ContentId pattern, String color, Region region) {
        public enum Kind { SCAR, TATTOO, MAGIC }

        public enum Region { FACE, HEAD, TORSO, LEFT_ARM, RIGHT_ARM, LEFT_LEG, RIGHT_LEG }
    }

    public static final int MAX_MARKINGS = 8;
    private static final Pattern HEX = Pattern.compile("#[0-9a-fA-F]{6}");

    public Appearance {
        features = Map.copyOf(features);
        markings = List.copyOf(markings);
    }

    /** Neutral default used when a species is chosen and before the player customises anything. */
    public static Appearance defaultFor(SpeciesDefinition species) {
        double height = Math.clamp(1.0, species.appearance().heightScaleMin(), species.appearance().heightScaleMax());
        Map<String, ContentId> features = new TreeMap<>();
        for (String slot : species.appearance().requiredSlots()) {
            features.put(slot, species.appearance().featureSlots().get(slot).getFirst());
        }
        return new Appearance(BodyType.CLASSIC, height, "#c58c6a", Optional.empty(), ContentId.of("eyes/round"), "#4a6b8a",
                ContentId.of("hair/short"), "#3b2a1e", Optional.empty(), features, List.of());
    }

    /** Returns German problem descriptions; empty if the appearance is valid for the species. */
    public List<String> validate(SpeciesDefinition species) {
        List<String> problems = new ArrayList<>();
        SpeciesDefinition.AppearanceRules rules = species.appearance();
        if (heightScale < rules.heightScaleMin() || heightScale > rules.heightScaleMax()) {
            problems.add("Größe " + heightScale + " liegt außerhalb von [" + rules.heightScaleMin() + ", " + rules.heightScaleMax() + "]");
        }
        checkColor(problems, "Hautfarbe", skinColor);
        checkColor(problems, "Augenfarbe", eyeColor);
        checkColor(problems, "Haarfarbe", hairColor);
        for (Map.Entry<String, ContentId> feature : features.entrySet()) {
            List<ContentId> allowed = rules.featureSlots().get(feature.getKey());
            if (allowed == null) {
                problems.add("Merkmal '" + feature.getKey() + "' ist für diese Spezies nicht verfügbar");
            } else if (!allowed.contains(feature.getValue())) {
                problems.add("Teil " + feature.getValue() + " ist für '" + feature.getKey() + "' nicht erlaubt");
            }
        }
        for (String slot : rules.requiredSlots()) {
            if (!features.containsKey(slot)) {
                problems.add("Merkmal '" + slot + "' ist für diese Spezies Pflicht");
            }
        }
        if (markings.size() > MAX_MARKINGS) {
            problems.add("Höchstens " + MAX_MARKINGS + " Markierungen erlaubt");
        }
        for (Marking marking : markings) {
            checkColor(problems, "Markierungsfarbe", marking.color());
        }
        return problems;
    }

    private static void checkColor(List<String> problems, String label, String color) {
        if (color == null || !HEX.matcher(color).matches()) {
            problems.add(label + " muss im Format #RRGGBB angegeben werden");
        }
    }
}
