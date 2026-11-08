package net.frosthain.core.character;

import net.frosthain.core.ContentId;
import net.frosthain.core.content.SpeciesDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Choices offered by the "Aussehen" step of the in-game editor (Design 2.0): colour swatches, hair styles, body
 * type, height steps and the species features. Everything chosen here is still validated by
 * {@link Appearance#validate} / {@link CharacterFactory} on the server.
 */
public final class AppearanceOptions {

    public static final List<String> SKIN_COLORS = List.of("#f2d3b1", "#e0b08a", "#c58c6a", "#a8714c", "#7d4f33",
            "#5a3622", "#b8c9b0", "#b04a4a", "#7d5ba6", "#6f8fa8");
    public static final List<String> EYE_COLORS = List.of("#4a6b8a", "#3d7a4a", "#7a5230", "#8a8a8a", "#2e2e2e",
            "#c9a227", "#b23a3a", "#6a4fa3");
    public static final List<String> HAIR_COLORS = List.of("#1c1410", "#3b2a1e", "#6b4423", "#a0662f", "#d9b26a",
            "#e8e0cc", "#8c8c8c", "#9a2f2f", "#2f4f9a");
    public static final List<ContentId> HAIR_STYLES = List.of(ContentId.of("hair/short"), ContentId.of("hair/long"),
            ContentId.of("hair/braided"), ContentId.of("hair/ponytail"), ContentId.of("hair/mohawk"), ContentId.of("hair/bald"));
    /** Height in percent steps of 2 within the species range. */
    public static final int HEIGHT_STEP_PERCENT = 2;

    private AppearanceOptions() {
    }

    /** Next entry in a cyclic list (direction +1 / -1). */
    public static <T> T cycle(List<T> options, T current, int direction) {
        int index = Math.max(0, options.indexOf(current));
        return options.get(Math.floorMod(index + direction, options.size()));
    }

    /** Height scale one step up or down, clamped to the species range and rounded to whole percent. */
    public static double stepHeight(SpeciesDefinition species, double current, int direction) {
        double next = Math.round(current * 100 + direction * HEIGHT_STEP_PERCENT) / 100.0;
        return Math.clamp(next, species.appearance().heightScaleMin(), species.appearance().heightScaleMax());
    }

    /** Options for one feature slot; optional slots also offer "none" (empty). */
    public static List<Optional<ContentId>> featureOptions(SpeciesDefinition species, String slot) {
        List<Optional<ContentId>> options = new ArrayList<>();
        if (!species.appearance().requiredSlots().contains(slot)) {
            options.add(Optional.empty());
        }
        species.appearance().featureSlots().getOrDefault(slot, List.of()).forEach(part -> options.add(Optional.of(part)));
        return options;
    }

    /** Returns a copy with one feature set (or removed when empty). */
    public static Appearance withFeature(Appearance appearance, String slot, Optional<ContentId> part) {
        Map<String, ContentId> features = new TreeMap<>(appearance.features());
        part.ifPresentOrElse(p -> features.put(slot, p), () -> features.remove(slot));
        return new Appearance(appearance.bodyType(), appearance.heightScale(), appearance.skinColor(), appearance.skinPattern(),
                appearance.eyeStyle(), appearance.eyeColor(), appearance.hairStyle(), appearance.hairColor(),
                appearance.beardStyle(), features, appearance.markings());
    }

    public static Appearance with(Appearance a, Appearance.BodyType bodyType, double height, String skin, String eyes,
                                  ContentId hairStyle, String hair) {
        return new Appearance(bodyType, height, skin, a.skinPattern(), a.eyeStyle(), eyes, hairStyle, hair, a.beardStyle(),
                a.features(), a.markings());
    }
}
