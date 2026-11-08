package net.frosthain.core.power;

import net.frosthain.core.ContentId;
import net.frosthain.core.character.CharacterSheet;
import net.frosthain.core.character.TalentRef;
import net.frosthain.core.content.ClassDefinition;
import net.frosthain.core.content.Modifier;
import net.frosthain.core.content.PowerDefinition;
import net.frosthain.core.content.ResourceDefinition;
import net.frosthain.core.registry.ContentRegistry;
import net.frosthain.core.registry.ContentType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Rules for active powers (milestone M4.1, first part): which powers a character knows, the class resource
 * with its maximum and regeneration, and whether a power can be used now (resource, cooldown).
 *
 * <p>Talents change resources through attribute effects named {@code <resource>_max} and {@code <resource>_regen}
 * (e.g. {@code frosthain:spell_power_max}), amount × rank, operation {@code add_value}.
 */
public final class PowerRules {

    /** Number of slots in the ability bar (reference image "Fähigkeitenleiste"). */
    public static final int SLOTS = 6;
    /** Ticks after taking or dealing damage during which the character counts as in combat. */
    public static final int COMBAT_TICKS = 100;

    public enum Problem { UNKNOWN_POWER, NOT_KNOWN, PASSIVE, ON_COOLDOWN, NOT_ENOUGH_RESOURCE }

    /** Runtime state of one character: current resource and remaining cooldown per power (ticks). */
    public record State(double resource, Map<ContentId, Integer> cooldowns) {
        public State {
            cooldowns = Map.copyOf(cooldowns);
        }

        public int cooldown(ContentId power) {
            return cooldowns.getOrDefault(power, 0);
        }
    }

    private final ContentRegistry registry;

    public PowerRules(ContentRegistry registry) {
        this.registry = registry;
    }

    /** Starting powers of all classes (in class order), then powers granted by allocated talents. */
    public List<ContentId> knownPowers(CharacterSheet sheet) {
        Set<ContentId> powers = new LinkedHashSet<>();
        for (ContentId classId : sheet.classLevels().keySet()) {
            registry.get(ContentType.CLASSES, classId).ifPresent(c -> powers.addAll(c.startingPowers()));
        }
        for (Modifier effect : talentEffects(sheet)) {
            if (effect instanceof Modifier.GrantPower grant) {
                powers.add(grant.power());
            }
        }
        powers.removeIf(id -> registry.get(ContentType.POWERS, id).map(p -> p.kind() == PowerDefinition.Kind.PASSIVE).orElse(true));
        return List.copyOf(powers);
    }

    /** The resource of the first class (multiclass pools follow with M3.4). */
    public Optional<ResourceDefinition> resource(CharacterSheet sheet) {
        return sheet.classLevels().keySet().stream().findFirst()
                .flatMap(id -> registry.get(ContentType.CLASSES, id))
                .flatMap(ClassDefinition::resource)
                .flatMap(id -> registry.get(ContentType.RESOURCES, id));
    }

    public int maximum(CharacterSheet sheet) {
        return resource(sheet).map(r -> (int) Math.round(r.baseMaximum() + talentBonus(sheet, r.id(), "_max"))).orElse(0);
    }

    public double regenerationPerSecond(CharacterSheet sheet) {
        return resource(sheet).map(r -> r.regenerationPerSecond() + talentBonus(sheet, r.id(), "_regen")).orElse(0.0);
    }

    public State fresh(CharacterSheet sheet) {
        return new State(maximum(sheet), Map.of());
    }

    /** Advances time: cooldowns run down, the resource regenerates (not in combat if the resource says so). */
    public State tick(State state, CharacterSheet sheet, int ticks, boolean inCombat) {
        Map<ContentId, Integer> cooldowns = new LinkedHashMap<>();
        state.cooldowns().forEach((power, remaining) -> {
            if (remaining > ticks) {
                cooldowns.put(power, remaining - ticks);
            }
        });
        double resource = Math.min(state.resource(), maximum(sheet));
        boolean regenerates = resource(sheet).map(r -> r.regeneratesInCombat() || !inCombat).orElse(false);
        if (regenerates) {
            resource = Math.min(maximum(sheet), resource + regenerationPerSecond(sheet) * ticks / 20.0);
        }
        return new State(resource, cooldowns);
    }

    public List<Problem> check(State state, CharacterSheet sheet, ContentId power) {
        Optional<PowerDefinition> definition = registry.get(ContentType.POWERS, power);
        if (definition.isEmpty()) {
            return List.of(Problem.UNKNOWN_POWER);
        }
        List<Problem> problems = new ArrayList<>();
        if (!knownPowers(sheet).contains(power)) {
            problems.add(Problem.NOT_KNOWN);
        }
        if (definition.get().kind() == PowerDefinition.Kind.PASSIVE) {
            problems.add(Problem.PASSIVE);
        }
        if (state.cooldown(power) > 0) {
            problems.add(Problem.ON_COOLDOWN);
        }
        if (definition.get().cost() > 0 && !paysWith(sheet, definition.get())) {
            problems.add(Problem.NOT_ENOUGH_RESOURCE);
        } else if (state.resource() + 1e-9 < definition.get().cost()) {
            problems.add(Problem.NOT_ENOUGH_RESOURCE);
        }
        return problems;
    }

    /** Spends the cost and starts the cooldown; call only after {@link #check} returned no problems. */
    public State use(State state, CharacterSheet sheet, ContentId power) {
        PowerDefinition definition = registry.get(ContentType.POWERS, power).orElseThrow();
        Map<ContentId, Integer> cooldowns = new LinkedHashMap<>(state.cooldowns());
        if (definition.cooldownTicks() > 0) {
            cooldowns.put(power, definition.cooldownTicks());
        }
        return new State(state.resource() - definition.cost(), cooldowns);
    }

    /** A power with a cost must use the character's resource (a fighter cannot cast with spell power). */
    private boolean paysWith(CharacterSheet sheet, PowerDefinition power) {
        return power.resource().isEmpty() || resource(sheet).map(r -> r.id().equals(power.resource().get())).orElse(false);
    }

    private double talentBonus(CharacterSheet sheet, ContentId resource, String suffix) {
        String attribute = resource + suffix;
        double bonus = 0;
        for (Map.Entry<TalentRef, Integer> talent : sheet.talents().entrySet()) {
            Optional<List<Modifier>> effects = registry.get(ContentType.TALENT_TREES, talent.getKey().tree())
                    .flatMap(tree -> tree.node(talent.getKey().node()))
                    .map(node -> node.effects());
            for (Modifier effect : effects.orElse(List.of())) {
                if (effect instanceof Modifier.AttributeModifier m && m.attribute().equals(attribute)
                        && m.operation() == Modifier.AttributeModifier.Operation.ADD_VALUE) {
                    bonus += m.amount() * talent.getValue();
                }
            }
        }
        return bonus;
    }

    private List<Modifier> talentEffects(CharacterSheet sheet) {
        List<Modifier> effects = new ArrayList<>();
        for (TalentRef ref : sheet.talents().keySet()) {
            registry.get(ContentType.TALENT_TREES, ref.tree()).flatMap(tree -> tree.node(ref.node()))
                    .ifPresent(node -> effects.addAll(node.effects()));
        }
        return effects;
    }
}
