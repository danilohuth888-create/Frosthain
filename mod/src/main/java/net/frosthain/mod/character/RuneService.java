package net.frosthain.mod.character;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.frosthain.core.ContentId;
import net.frosthain.core.content.RuneDefinition;
import net.frosthain.core.quest.QuestLog;
import net.frosthain.core.registry.ContentType;
import net.frosthain.core.rune.RuneRules;
import net.frosthain.mod.FrosthainMod;
import net.frosthain.mod.network.InscribeRunePayload;
import net.frosthain.mod.network.RuneAltarPayload;
import net.frosthain.ui.Theme;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rune magic (M4.2, first part). Once "Die erste Rune" is completed, the rune stone of the shrine is a rune altar.
 * Inscribing a rune consumes its materials (checked by {@link RuneRules} on the server) and gives a rune seal:
 * paper with the rune id in its custom data. Using the seal releases the effect ({@link RuneEffects}).
 */
public final class RuneService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FrosthainMod.MOD_ID);
    private static final String SEAL_KEY = "frosthain_rune";
    private static final ContentId FIRST_RUNE = ContentId.of("first_rune");
    private static final double ALTAR_REACH = 8;

    /** Where each player opened the altar; inscribing only works close to it. */
    private static final Map<UUID, BlockPos> ALTARS = new ConcurrentHashMap<>();

    private RuneService() {
    }

    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(RuneAltarPayload.TYPE, RuneAltarPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(InscribeRunePayload.TYPE, InscribeRunePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(InscribeRunePayload.TYPE, (payload, context) -> context.server().execute(() -> {
            try {
                inscribe(context.player(), ContentId.parse(payload.rune()));
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Ungültige Rune von {}: {}", context.player().getName().getString(), payload.rune());
            }
        }));
        UseBlockCallback.EVENT.register((player, level, hand, hit) ->
                player instanceof ServerPlayer serverPlayer && isAltar(serverPlayer, hit.getBlockPos())
                        && openAltar(serverPlayer, hit.getBlockPos()) ? InteractionResult.SUCCESS : InteractionResult.PASS);
        UseItemCallback.EVENT.register((player, level, hand) ->
                player instanceof ServerPlayer serverPlayer && useSeal(serverPlayer, hand) ? InteractionResult.SUCCESS : InteractionResult.PASS);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> ALTARS.remove(handler.getPlayer().getUUID()));
        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> dispatcher.register(
                Commands.literal("frosthain").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("runenaltar").executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            return openAltar(player, player.blockPosition()) ? 1 : 0;
                        }))));
    }

    private static RuneRules rules() {
        return new RuneRules(FrosthainMod.content());
    }

    /** The shrine's rune stone (or the crystal on it), after "Die erste Rune". */
    private static boolean isAltar(ServerPlayer player, BlockPos pos) {
        return QuestSites.shrine(player).filter(center -> pos.equals(center) || pos.equals(center.above())).isPresent()
                && QuestService.log(player).isCompleted(FIRST_RUNE);
    }

    public static boolean openAltar(ServerPlayer player, BlockPos altar) {
        if (rules().known(QuestService.log(player)).isEmpty()) {
            player.sendOverlayMessage(Component.translatable("gui.frosthain.runes.silent"));
            return false;
        }
        ALTARS.put(player.getUUID(), altar.immutable());
        ServerPlayNetworking.send(player, new RuneAltarPayload(true));
        player.level().playSound(null, altar, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0f, 0.8f);
        return true;
    }

    /** Server side of "Herstellen". Returns true if a seal was created. */
    public static boolean inscribe(ServerPlayer player, ContentId runeId) {
        BlockPos altar = ALTARS.get(player.getUUID());
        if (altar == null || !player.blockPosition().closerThan(altar, ALTAR_REACH)) {
            player.sendOverlayMessage(Component.translatable("gui.frosthain.runes.too_far"));
            return false;
        }
        QuestLog log = QuestService.log(player);
        Optional<RuneRules.Problem> problem = rules().check(runeId, log, inventoryCounts(player.getInventory()));
        if (problem.isPresent()) {
            player.sendOverlayMessage(Component.translatable(switch (problem.get()) {
                case UNKNOWN_RUNE, NOT_KNOWN -> "gui.frosthain.runes.not_known";
                case MISSING_MATERIALS -> "gui.frosthain.runes.missing";
            }));
            return false;
        }
        RuneDefinition rune = FrosthainMod.content().get(ContentType.RUNES, runeId).orElseThrow();
        for (RuneDefinition.Material material : rune.materials()) {
            consume(player.getInventory(), material.item(), material.count());
        }
        ItemStack seal = seal(rune);
        if (!player.getInventory().add(seal)) {
            player.level().addFreshEntity(new ItemEntity(player.level(), player.getX(), player.getY() + 0.5, player.getZ(), seal));
        }
        ServerLevel level = player.level();
        level.sendParticles(ParticleTypes.ENCHANT, altar.getX() + 0.5, altar.getY() + 1.5, altar.getZ() + 0.5, 50, 0.5, 0.6, 0.5, 0.6);
        level.playSound(null, altar, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.4f, 1.2f);
        player.sendOverlayMessage(Component.translatable("gui.frosthain.runes.inscribed", Component.translatable(rune.translationKey())));
        LOGGER.info("Rune {} von {} eingraviert", rune.id(), player.getName().getString());
        return true;
    }

    /** Item id → total count in the player's inventory. */
    public static Map<String, Integer> inventoryCounts(Inventory inventory) {
        Map<String, Integer> counts = new HashMap<>();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && sealRune(stack).isEmpty()) {
                counts.merge(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), stack.getCount(), Integer::sum);
            }
        }
        return counts;
    }

    private static void consume(Inventory inventory, String item, int count) {
        int left = count;
        for (int slot = 0; slot < inventory.getContainerSize() && left > 0; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && sealRune(stack).isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(item)) {
                int taken = Math.min(left, stack.getCount());
                stack.shrink(taken);
                left -= taken;
            }
        }
        inventory.setChanged();
    }

    /** A rune seal: glinting paper named after the rune, with description and usage hint. */
    public static ItemStack seal(RuneDefinition rune) {
        ItemStack stack = new ItemStack(Items.PAPER);
        stack.set(DataComponents.ITEM_NAME, Component.translatable("item.frosthain.rune_seal", Component.translatable(rune.translationKey()))
                .withStyle(style -> style.withColor(Theme.RUNE_BLUE & 0xFFFFFF)));
        stack.set(DataComponents.LORE, new ItemLore(List.of(
                Component.translatable(rune.translationKey() + ".desc").withStyle(ChatFormatting.GRAY),
                Component.translatable("item.frosthain.rune_seal.use").withStyle(ChatFormatting.DARK_AQUA))));
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        CompoundTag tag = new CompoundTag();
        tag.putString(SEAL_KEY, rune.id().toString());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    public static Optional<ContentId> sealRune(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return Optional.empty();
        }
        return data.copyTag().getString(SEAL_KEY).map(ContentId::parse);
    }

    /** Right-click with a seal: release the rune and use the seal up. */
    public static boolean useSeal(ServerPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Optional<RuneDefinition> rune = sealRune(stack).flatMap(id -> FrosthainMod.content().get(ContentType.RUNES, id));
        if (rune.isEmpty()) {
            return false;
        }
        RuneEffects.release(player, rune.get());
        if (!player.hasInfiniteMaterials()) {
            stack.shrink(1);
        }
        return true;
    }
}
