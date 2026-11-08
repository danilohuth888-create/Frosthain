package net.frosthain.mod.character;

import net.frosthain.core.character.CharacterSheet;
import net.frosthain.core.character.TalentRef;
import net.frosthain.core.content.Modifier;
import net.frosthain.core.content.TalentTreeDefinition;
import net.frosthain.core.registry.ContentRegistry;
import net.frosthain.core.registry.ContentType;
import net.frosthain.mod.FrosthainMod;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Makes attribute talents work in the game: all allocated nodes are summed per attribute and operation
 * (amount × rank) and applied as one permanent modifier each. Other effect types (powers, flags, resistances,
 * skills) follow with the ability system (milestone M3.2/M4.1).
 */
public final class TalentEffects {

    private static final Logger LOGGER = LoggerFactory.getLogger(FrosthainMod.MOD_ID);

    private TalentEffects() {
    }

    public static void apply(ServerPlayer player, CharacterSheet sheet, ContentRegistry registry) {
        Map<String, Double> totals = new TreeMap<>();
        for (Map.Entry<TalentRef, Integer> talent : sheet.talents().entrySet()) {
            registry.get(ContentType.TALENT_TREES, talent.getKey().tree())
                    .flatMap(tree -> tree.node(talent.getKey().node()))
                    .ifPresent(node -> node.effects().forEach(effect -> {
                        if (effect instanceof Modifier.AttributeModifier attribute) {
                            totals.merge(key(attribute.attribute(), attribute.operation()), attribute.amount() * talent.getValue(), Double::sum);
                        }
                    }));
        }
        for (String attributeId : talentAttributes(registry)) {
            Optional<AttributeInstance> instance = instance(player, attributeId);
            if (instance.isEmpty()) {
                continue;
            }
            for (Modifier.AttributeModifier.Operation operation : Modifier.AttributeModifier.Operation.values()) {
                Identifier id = modifierId(operation);
                instance.get().removeModifier(id);
                Double amount = totals.get(key(attributeId, operation));
                if (amount != null && amount != 0) {
                    instance.get().addOrReplacePermanentModifier(new AttributeModifier(id, amount, minecraftOperation(operation)));
                }
            }
        }
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    /** Every attribute any talent can change; modifiers on these are removed before re-applying. */
    private static Set<String> talentAttributes(ContentRegistry registry) {
        Set<String> attributes = new TreeSet<>();
        for (TalentTreeDefinition tree : registry.all(ContentType.TALENT_TREES).values()) {
            for (TalentTreeDefinition.TalentNode node : tree.nodes().values()) {
                for (Modifier effect : node.effects()) {
                    if (effect instanceof Modifier.AttributeModifier attribute) {
                        attributes.add(attribute.attribute());
                    }
                }
            }
        }
        return attributes;
    }

    private static Optional<AttributeInstance> instance(ServerPlayer player, String attributeId) {
        Identifier id = Identifier.tryParse(attributeId);
        Optional<Holder.Reference<Attribute>> holder = id == null ? Optional.empty() : BuiltInRegistries.ATTRIBUTE.get(id);
        if (holder.isEmpty()) {
            LOGGER.debug("Attribut {} gibt es in dieser Minecraft-Version nicht – Talent-Effekt übersprungen", attributeId);
            return Optional.empty();
        }
        return Optional.ofNullable(player.getAttribute(holder.get()));
    }

    private static String key(String attribute, Modifier.AttributeModifier.Operation operation) {
        return attribute + "|" + operation;
    }

    private static Identifier modifierId(Modifier.AttributeModifier.Operation operation) {
        return FrosthainMod.id("talents/" + operation.name().toLowerCase(java.util.Locale.ROOT));
    }

    private static AttributeModifier.Operation minecraftOperation(Modifier.AttributeModifier.Operation operation) {
        return switch (operation) {
            case ADD_VALUE -> AttributeModifier.Operation.ADD_VALUE;
            case ADD_MULTIPLIED_BASE -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
            case ADD_MULTIPLIED_TOTAL -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
        };
    }
}
