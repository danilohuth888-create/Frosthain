package net.frosthain.mod.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.frosthain.core.ContentId;
import net.frosthain.core.character.Appearance;
import net.frosthain.core.character.AppearanceOptions;
import net.frosthain.core.character.CharacterSheet;
import net.frosthain.core.character.TalentRef;
import net.frosthain.core.quest.QuestLog;
import net.frosthain.core.registry.ContentType;
import net.frosthain.mod.FrosthainMod;
import net.frosthain.mod.character.CharacterService;
import net.frosthain.mod.character.DialogueService;
import net.frosthain.mod.character.PowerService;
import net.frosthain.mod.character.QuestService;
import net.frosthain.mod.character.QuestSites;
import net.frosthain.mod.character.RuneService;
import net.frosthain.mod.world.FrozenCrypt;
import net.frosthain.mod.world.StarterVillage;
import net.frosthain.mod.client.ClientCharacter;
import net.frosthain.mod.client.ClientPowers;
import net.frosthain.mod.client.ClientQuests;
import net.frosthain.mod.client.screen.CharacterCreationScreen;
import net.frosthain.mod.client.screen.DialogueScreen;
import net.frosthain.mod.client.screen.PowerDetailsScreen;
import net.frosthain.mod.client.screen.FrosthainTitleScreen;
import net.frosthain.mod.client.screen.QuestJournalScreen;
import net.frosthain.mod.client.screen.TalentTreeScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import net.frosthain.mod.client.screen.RuneAltarScreen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import java.util.Map;
import java.util.Optional;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Real client test: boots Minecraft with FROSTHAIN, checks the main menu, walks through character creation in a
 * new singleplayer world, verifies that the server stored the character and takes screenshots of every view.
 * Runs headless in CI (xvfb + Mesa) and locally with the real graphics card.
 */
public final class FrosthainClientGameTest implements FabricClientGameTest {

    @Override
    public void runTest(ClientGameTestContext context) {
        if (FrosthainMod.content() == null || FrosthainMod.content().all(ContentType.CLASSES).isEmpty()) {
            throw new AssertionError("FROSTHAIN-Inhalte wurden im Client nicht geladen");
        }
        context.waitForScreen(FrosthainTitleScreen.class);
        context.takeScreenshot("frosthain-01-hauptmenue");

        // clickScreenButton only finds vanilla Button instances; FROSTHAIN buttons are AbstractButtons.
        context.runOnClient(client -> client.gui.setScreen(
                new CharacterCreationScreen(client.gui.screen(), CharacterCreationScreen.Mode.PREVIEW)));
        context.waitForScreen(CharacterCreationScreen.class);
        context.takeScreenshot("frosthain-02-vorschau-spezies");
        context.runOnClient(client -> client.gui.screen().onClose());
        context.waitForScreen(FrosthainTitleScreen.class);

        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            context.waitForScreen(CharacterCreationScreen.class);
            context.waitTicks(20);
            context.takeScreenshot("frosthain-03-spezies");
            context.runOnClient(client -> {
                CharacterCreationScreen screen = creation(client);
                screen.select(ContentId.of("elf"));
                screen.show(CharacterCreationScreen.Step.APPEARANCE);
                Appearance a = screen.appearance();
                screen.setAppearance(AppearanceOptions.withFeature(AppearanceOptions.with(a, Appearance.BodyType.SLIM, 1.04,
                                "#e0b08a", "#3d7a4a", ContentId.of("hair/braided"), "#d9b26a"),
                        "ears", Optional.of(ContentId.parse("frosthain:ears/pointed_long"))));
            });
            context.waitTicks(2);
            context.takeScreenshot("frosthain-03b-aussehen");
            context.runOnClient(client -> {
                CharacterCreationScreen screen = creation(client);
                screen.select(ContentId.of("elf"));
                screen.show(CharacterCreationScreen.Step.CLASS);
                screen.select(ContentId.of("rune_mage"));
            });
            context.takeScreenshot("frosthain-04-klasse");
            context.runOnClient(client -> {
                creation(client).show(CharacterCreationScreen.Step.BACKGROUND);
                creation(client).select(ContentId.of("sage"));
            });
            context.takeScreenshot("frosthain-05-hintergrund");
            context.runOnClient(client -> creation(client).show(CharacterCreationScreen.Step.FINISH));
            context.takeScreenshot("frosthain-06-fertig");

            context.runOnClient(client -> creation(client).submit());
            context.waitFor(client -> ClientCharacter.state().isPresent() && client.gui.screen() == null);
            // The opening main quest starts with the character; the tracker is collapsed by default.
            context.waitFor(client -> ClientQuests.log().isActive(AWAKENING));
            context.waitTicks(40);
            context.takeScreenshot("frosthain-07-hud");
            context.runOnClient(client -> ClientQuests.toggleTracker());
            context.waitTicks(2);
            context.takeScreenshot("frosthain-07b-questtracker");

            singleplayer.getServer().runOnServer(server -> {
                CharacterSheet sheet = CharacterService.sheet(server.getPlayerList().getPlayers().getFirst())
                        .orElseThrow(() -> new AssertionError("Server hat keinen Charakter gespeichert"));
                if (!sheet.classLevels().containsKey(ContentId.of("rune_mage")) || !sheet.species().equals(ContentId.of("elf"))) {
                    throw new AssertionError("Gespeicherter Charakter weicht ab: " + sheet.species() + " " + sheet.classLevels());
                }
                Appearance stored = sheet.appearance();
                if (!stored.hairStyle().equals(ContentId.of("hair/braided")) || !stored.hairColor().equals("#d9b26a")
                        || stored.bodyType() != Appearance.BodyType.SLIM
                        || !ContentId.parse("frosthain:ears/pointed_long").equals(stored.features().get("ears"))) {
                    throw new AssertionError("Gewähltes Aussehen wurde nicht gespeichert: " + stored);
                }
            });

            context.setScreen(() -> new CharacterCreationScreen(null, CharacterCreationScreen.Mode.VIEW));
            context.takeScreenshot("frosthain-08-charakter");
            context.setScreen(() -> null);

            // Talent tree (K): 2000 EP = level 4 = 4 points; unlock two talents and check rules and effects on the server.
            singleplayer.getServer().runCommand("execute as @a run frosthain ep 2000");
            context.waitFor(client -> ClientCharacter.sheet().map(s -> s.characterLevel() >= 4).orElse(false));
            // Level-up: title "Stufe 4" in the heading font, subtitle with the talent tree key.
            context.waitTicks(15);
            context.takeScreenshot("frosthain-08b-stufenaufstieg");
            context.setScreen(TalentTreeScreen::new);
            context.waitTicks(5);
            context.takeScreenshot("frosthain-09-talentbaum");
            unlock(context, "combat_stance");
            unlock(context, "toughness");
            context.waitTicks(5);
            context.takeScreenshot("frosthain-10-talente-freigeschaltet");
            singleplayer.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                CharacterSheet sheet = CharacterService.sheet(player).orElseThrow();
                if (!sheet.talents().containsKey(new TalentRef(ContentId.of("general"), "combat_stance"))) {
                    throw new AssertionError("Talent wurde nicht gespeichert: " + sheet.talents());
                }
                boolean applied = player.getAttribute(Attributes.ATTACK_DAMAGE).getModifiers().stream()
                        .anyMatch(modifier -> modifier.id().getNamespace().equals(FrosthainMod.MOD_ID));
                if (!applied) {
                    throw new AssertionError("Kampfhaltung wirkt nicht auf den Angriffsschaden");
                }
            });
            context.setScreen(() -> null);

            // Quest journal (J); objectives via the test command until Eldrin and the shrine exist in the world.
            context.setScreen(QuestJournalScreen::new);
            context.waitTicks(5);
            context.takeScreenshot("frosthain-11-questjournal");
            context.setScreen(() -> null);
            long experienceBefore = singleplayer.getServer().computeOnServer(server ->
                    CharacterService.sheet(server.getPlayerList().getPlayers().getFirst()).orElseThrow().experience());
            // Eldrin stands in the world; talking to him (server-side session) fulfils the first objective.
            singleplayer.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                boolean eldrinPresent = false;
                for (Entity entity : player.level().getAllEntities()) {
                    eldrinPresent |= entity.entityTags().contains("frosthain.npc.eldrin");
                }
                if (!eldrinPresent) {
                    throw new AssertionError("Eldrin wurde nicht in der Welt platziert");
                }
                DialogueService.open(player, ContentId.of("eldrin"));
            });
            context.waitForScreen(DialogueScreen.class);
            context.waitTicks(5);
            context.takeScreenshot("frosthain-11b-dialog-eldrin");
            context.runOnClient(client -> {
                DialogueScreen screen = (DialogueScreen) client.gui.screen();
                int ready = screen.state().choices().indexOf("ready");
                if (ready < 0) {
                    throw new AssertionError("Antwort 'Ich bin bereit' fehlt: " + screen.state().choices());
                }
                screen.choose(ready);
            });
            context.waitFor(client -> client.gui.screen() instanceof DialogueScreen screen && screen.state().atEnd());
            context.takeScreenshot("frosthain-11c-dialog-antwort");
            context.runOnClient(client -> ((DialogueScreen) client.gui.screen()).choose(0));
            context.waitFor(client -> client.gui.screen() == null);
            singleplayer.getServer().runOnServer(server -> {
                QuestLog log = QuestService.log(server.getPlayerList().getPlayers().getFirst());
                if (log.progress(AWAKENING).map(p -> p.count("talk_eldrin")).orElse(0) != 1) {
                    throw new AssertionError("Gespräch mit Eldrin hat das Questziel nicht erfüllt: " + log);
                }
            });
            // The shrine was built north of the player when "find the old shrine" became current; walk there (teleport).
            BlockPos shrine = singleplayer.getServer().computeOnServer(server -> QuestSites.shrine(server.getPlayerList().getPlayers().getFirst())
                    .orElseThrow(() -> new AssertionError("Alter Schrein wurde nicht errichtet")));
            context.waitFor(client -> ClientQuests.marker().present());
            context.waitTicks(25);
            context.takeScreenshot("frosthain-12a-wegweiser");
            String start = singleplayer.getServer().computeOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                return player.getX() + " " + player.getY() + " " + player.getZ() + " " + player.getYRot() + " " + player.getXRot();
            });
            singleplayer.getServer().runCommand("tp @a " + (shrine.getX() + 0.5) + " " + shrine.getY() + " " + (shrine.getZ() + 5.5) + " 180 20");
            context.waitFor(client -> ClientQuests.log().isCompleted(AWAKENING) && ClientQuests.log().isActive(FIRST_RUNE));
            singleplayer.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                QuestLog log = QuestService.log(player);
                if (!log.isCompleted(AWAKENING) || !log.isActive(FIRST_RUNE)) {
                    throw new AssertionError("Questlog auf dem Server weicht ab: " + log);
                }
                long gained = CharacterService.sheet(player).orElseThrow().experience() - experienceBefore;
                if (gained != 150) {
                    throw new AssertionError("Questbelohnung falsch: " + gained + " EP statt 150");
                }
            });
            context.waitTicks(10);
            context.takeScreenshot("frosthain-12-schrein-folgequest");
            // Touching the rune stone completes "Die erste Rune" (+200 EP).
            singleplayer.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                long before = CharacterService.sheet(player).orElseThrow().experience();
                if (!QuestSites.touch(player, shrine)) {
                    throw new AssertionError("Runenstein hat nicht reagiert");
                }
                long gained = CharacterService.sheet(player).orElseThrow().experience() - before;
                if (!QuestService.log(player).isCompleted(FIRST_RUNE) || gained != 200) {
                    throw new AssertionError("Die erste Rune nicht abgeschlossen oder EP falsch: " + gained);
                }
            });
            // Follow-up "Die gefrorene Gruft": the crypt is built south-east of the village, the waypoint leads there.
            context.waitFor(client -> ClientQuests.log().isActive(FROZEN_CRYPT) && ClientQuests.marker().present());
            FrozenCrypt.Crypt crypt = singleplayer.getServer().computeOnServer(server ->
                    FrozenCrypt.crypt(server.overworld()).orElseThrow(() -> new AssertionError("Gruft wurde nicht errichtet")));
            singleplayer.getServer().runCommand("effect give @a minecraft:resistance 600 4 true");
            singleplayer.getServer().runCommand("tp @a " + (crypt.entrance().getX() + 0.5) + " " + crypt.entrance().getY()
                    + " " + (crypt.entrance().getZ() - 3.5) + " 0 25");
            context.waitFor(client -> ClientQuests.log().progress(FROZEN_CRYPT).map(p -> p.count("find_crypt") == 1).orElse(false));
            context.waitTicks(20);
            context.takeScreenshot("frosthain-14a-gruft-eingang");
            // Entering the burial chamber wakes the Frost Warden (boss bar, 120 health).
            singleplayer.getServer().runCommand("tp @a " + (crypt.chamber().getX() + 0.5) + " " + crypt.chamber().getY()
                    + " " + (crypt.chamber().getZ() - 5.5) + " 0 5");
            boolean awake = false;
            for (int i = 0; i < 40 && !awake; i++) {
                context.waitTicks(5);
                awake = singleplayer.getServer().computeOnServer(server -> !server.overworld().getEntitiesOfClass(Mob.class,
                        crypt.chamberBox().inflate(6), e -> e.entityTags().contains(FrozenCrypt.BOSS_TAG)).isEmpty());
            }
            if (!awake) {
                throw new AssertionError("Frostwächter ist beim Betreten der Grabkammer nicht erwacht");
            }
            context.waitTicks(20);
            context.takeScreenshot("frosthain-14b-frostwaechter");
            singleplayer.getServer().runOnServer(server -> {
                Mob boss = warden(server.overworld(), crypt);
                if (boss.getMaxHealth() != 120) {
                    throw new AssertionError("Frostwächter hat " + boss.getMaxHealth() + " statt 120 Leben");
                }
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                boss.hurtServer(server.overworld(), player.damageSources().playerAttack(player), 80);
            });
            context.waitTicks(15);
            singleplayer.getServer().runOnServer(server -> {
                Mob boss = warden(server.overworld(), crypt);
                long servants = server.overworld().getEntitiesOfClass(Mob.class, crypt.chamberBox().inflate(6),
                        e -> e.entityTags().contains(FrozenCrypt.SERVANT_TAG)).size();
                if (!boss.entityTags().contains(FrozenCrypt.PHASE_TWO_TAG) || servants != 2) {
                    throw new AssertionError("Phase 2 nicht ausgelöst (Leben " + boss.getHealth() + ", Diener " + servants + ")");
                }
            });
            context.waitTicks(10);
            context.takeScreenshot("frosthain-14c-frostwaechter-phase-2");
            long experienceBeforeBoss = singleplayer.getServer().computeOnServer(server ->
                    CharacterService.sheet(server.getPlayerList().getPlayers().getFirst()).orElseThrow().experience());
            singleplayer.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                warden(server.overworld(), crypt).hurtServer(server.overworld(), player.damageSources().playerAttack(player), 1000);
            });
            context.waitFor(client -> ClientQuests.log().isCompleted(FROZEN_CRYPT) && !ClientQuests.marker().present());
            singleplayer.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                long gained = CharacterService.sheet(player).orElseThrow().experience() - experienceBeforeBoss;
                long servants = server.overworld().getEntitiesOfClass(Mob.class, crypt.chamberBox().inflate(6),
                        e -> e.entityTags().contains(FrozenCrypt.SERVANT_TAG)).size();
                if (gained < 300 || servants != 0) {
                    throw new AssertionError("Boss-Belohnung " + gained + " EP (mind. 300), Diener übrig: " + servants);
                }
            });
            context.waitTicks(10);
            context.takeScreenshot("frosthain-14d-frostwaechter-besiegt");

            // Rune altar: after "Die erste Rune" the shrine's rune stone is an altar; inscribing consumes the materials.
            singleplayer.getServer().runCommand("tp @a " + (shrine.getX() + 0.5) + " " + shrine.getY() + " " + (shrine.getZ() + 3.5) + " 180 30");
            singleplayer.getServer().runCommand("give @a minecraft:amethyst_shard 2");
            singleplayer.getServer().runCommand("give @a minecraft:coal 3");
            context.waitTicks(10);
            singleplayer.getServer().runOnServer(server -> {
                if (!RuneService.openAltar(server.getPlayerList().getPlayers().getFirst(), shrine)) {
                    throw new AssertionError("Runenaltar öffnet sich nicht");
                }
            });
            context.waitForScreen(RuneAltarScreen.class);
            context.waitTicks(10);
            context.takeScreenshot("frosthain-14e-runenaltar");
            singleplayer.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                Map<String, Integer> before = RuneService.inventoryCounts(player.getInventory());
                if (!RuneService.inscribe(player, ContentId.of("fire"))) {
                    throw new AssertionError("Rune des Feuers ließ sich nicht herstellen: " + before);
                }
                Map<String, Integer> after = RuneService.inventoryCounts(player.getInventory());
                if (after.getOrDefault("minecraft:coal", 0) != before.get("minecraft:coal") - 3
                        || after.getOrDefault("minecraft:amethyst_shard", 0) != before.get("minecraft:amethyst_shard") - 1) {
                    throw new AssertionError("Materialien falsch abgezogen: " + before + " → " + after);
                }
                if (RuneService.inscribe(player, ContentId.of("fire"))) {
                    throw new AssertionError("Zweite Rune ohne Kohle hergestellt");
                }
            });
            context.waitTicks(15);
            context.takeScreenshot("frosthain-14f-runensiegel");
            context.setScreen(() -> null);
            // Releasing the seal: the rune of fire burns and hurts a creature next to the player, the seal is used up.
            singleplayer.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                int seal = -1;
                for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                    if (RuneService.sealRune(player.getInventory().getItem(slot)).isPresent()) {
                        seal = slot;
                    }
                }
                if (seal < 0) {
                    throw new AssertionError("Kein Runensiegel im Inventar");
                }
                ItemStack stack = player.getInventory().getItem(seal).copy();
                player.getInventory().getItem(seal).setCount(0);
                player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                Mob husk = EntityTypes.HUSK.create(player.level(), EntitySpawnReason.EVENT);
                husk.snapTo(player.getX() + 2, player.getY(), player.getZ(), 0, 0);
                husk.setNoAi(true);
                player.level().addFreshEntity(husk);
                if (!RuneService.useSeal(player, InteractionHand.MAIN_HAND)) {
                    throw new AssertionError("Runensiegel wirkt nicht");
                }
                boolean consumed = player.hasInfiniteMaterials() || player.getMainHandItem().isEmpty();
                if (!husk.isOnFire() || husk.getHealth() >= husk.getMaxHealth() || !consumed) {
                    throw new AssertionError("Feuerrune: brennt " + husk.isOnFire() + ", Leben " + husk.getHealth() + ", verbraucht " + consumed);
                }
                husk.discard();
            });
            singleplayer.getServer().runCommand("effect clear @a minecraft:resistance");
            singleplayer.getServer().runCommand("tp @a " + start);
            context.waitTicks(10);
            context.setScreen(QuestJournalScreen::new);
            context.runOnClient(client -> ((QuestJournalScreen) client.gui.screen()).show(QuestJournalScreen.Category.DONE));
            context.waitTicks(5);
            context.takeScreenshot("frosthain-13-questjournal-abgeschlossen");
            context.setScreen(() -> null);

            // Starting village: built at the spawn; the notice board starts "Verlorene Vorräte", three crates finish it (+80 EP).
            StarterVillage.Village village = singleplayer.getServer().computeOnServer(server ->
                    StarterVillage.village(server.overworld()).orElseThrow(() -> new AssertionError("Startdorf wurde nicht errichtet")));
            singleplayer.getServer().runCommand("title @a clear");
            singleplayer.getServer().runCommand("effect give @a minecraft:slow_falling 15 0 true");
            singleplayer.getServer().runCommand("tp @a " + (village.center().getX() + 0.5) + " " + (village.center().getY() + 14)
                    + " " + (village.center().getZ() + 26.5) + " 180 30");
            context.waitTicks(40);
            context.takeScreenshot("frosthain-13a-startdorf");
            singleplayer.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                if (!StarterVillage.use(player, village.board()) || !QuestService.log(player).isActive(LOST_SUPPLIES)) {
                    throw new AssertionError("Anschlagbrett startet 'Verlorene Vorräte' nicht");
                }
            });
            context.waitFor(client -> ClientQuests.log().isActive(LOST_SUPPLIES) && ClientQuests.marker().present());
            context.waitTicks(10);
            context.takeScreenshot("frosthain-13b-vorraete-wegweiser");
            singleplayer.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                long before = CharacterService.sheet(player).orElseThrow().experience();
                for (BlockPos crate : village.crates()) {
                    if (!StarterVillage.use(player, crate)) {
                        throw new AssertionError("Vorratskiste reagiert nicht: " + crate);
                    }
                }
                long gained = CharacterService.sheet(player).orElseThrow().experience() - before;
                if (!QuestService.log(player).isCompleted(LOST_SUPPLIES) || gained != 80) {
                    throw new AssertionError("'Verlorene Vorräte' nicht abgeschlossen oder EP falsch: " + gained);
                }
            });
            context.waitFor(client -> ClientQuests.log().isCompleted(LOST_SUPPLIES) && !ClientQuests.marker().present());
            singleplayer.getServer().runCommand("tp @a " + start);
            context.waitTicks(10);

            // Ability bar: the rune mage starts with Runenstoß in slot 1; a golem without AI is the target.
            context.waitFor(client -> ClientPowers.state().map(s -> s.powers().contains("frosthain:rune_strike") && s.current() == s.maximum()).orElse(false));
            singleplayer.getServer().runCommand("execute as @a at @s run summon minecraft:iron_golem ^ ^ ^7 {NoAI:1b,Tags:[\"frosthain_test_target\"]}");
            context.waitTicks(10);
            context.runOnClient(client -> ClientPowers.use(client, 0));
            context.waitFor(client -> ClientPowers.state().map(s -> s.cooldowns().getFirst() > 0).orElse(false));
            context.waitTicks(4);
            context.takeScreenshot("frosthain-14-runenstoss");
            singleplayer.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                double resource = PowerService.state(player).orElseThrow().resource();
                if (resource > 61 || resource < 59) {
                    throw new AssertionError("Runenstoß hat nicht 40 Zauberkraft gekostet: " + resource);
                }
                LivingEntity golem = null;
                for (Entity entity : player.level().getAllEntities()) {
                    if (entity.entityTags().contains("frosthain_test_target")) {
                        golem = (LivingEntity) entity;
                    }
                }
                if (golem == null || golem.getHealth() >= golem.getMaxHealth()) {
                    throw new AssertionError("Runenstoß hat das Ziel nicht getroffen: " + (golem == null ? "kein Ziel" : golem.getHealth()));
                }
            });
            // A second use during the cooldown is refused and costs nothing.
            context.runOnClient(client -> ClientPowers.use(client, 0));
            context.waitTicks(10);
            singleplayer.getServer().runOnServer(server -> {
                double resource = PowerService.state(server.getPlayerList().getPlayers().getFirst()).orElseThrow().resource();
                if (resource < 59) {
                    throw new AssertionError("Fähigkeit wurde trotz Abklingzeit ausgelöst: " + resource);
                }
            });

            // Experience from combat: defeating a husk (20 health, hostile) with Runenstoß gives 20 × 2.5 = 50 EP.
            singleplayer.getServer().runCommand("kill @e[tag=frosthain_test_target]");
            singleplayer.getServer().runCommand("execute as @a at @s run summon minecraft:husk ^ ^ ^5 {NoAI:1b,Health:1f,Tags:[\"frosthain_test_husk\"]}");
            long experienceBeforeFight = singleplayer.getServer().computeOnServer(server ->
                    CharacterService.sheet(server.getPlayerList().getPlayers().getFirst()).orElseThrow().experience());
            context.waitFor(client -> ClientPowers.state().map(s -> s.cooldowns().getFirst() == 0).orElse(false), 400);
            context.runOnClient(client -> ClientPowers.use(client, 0));
            singleplayer.getServer().waitFor(server -> CharacterService.sheet(server.getPlayerList().getPlayers().getFirst())
                    .orElseThrow().experience() >= experienceBeforeFight + 50, 100);
            singleplayer.getServer().runOnServer(server -> {
                long gained = CharacterService.sheet(server.getPlayerList().getPlayers().getFirst()).orElseThrow().experience() - experienceBeforeFight;
                if (gained != 50) {
                    throw new AssertionError("Kampf-EP falsch: " + gained + " statt 50");
                }
            });
            context.waitTicks(10);
            context.takeScreenshot("frosthain-15-kampf-ep");

            context.setScreen(() -> new PowerDetailsScreen(new net.minecraft.client.KeyMapping[0]));
            context.waitTicks(5);
            context.takeScreenshot("frosthain-16-faehigkeitsdetails");
            context.setScreen(() -> null);
        }
    }

    private static final ContentId AWAKENING = ContentId.of("awakening");
    private static final ContentId FIRST_RUNE = ContentId.of("first_rune");
    private static final ContentId LOST_SUPPLIES = ContentId.of("lost_supplies");
    private static final ContentId FROZEN_CRYPT = ContentId.of("frozen_crypt");

    private static Mob warden(ServerLevel level, FrozenCrypt.Crypt crypt) {
        return level.getEntitiesOfClass(Mob.class, crypt.chamberBox().inflate(6), e -> e.entityTags().contains(FrozenCrypt.BOSS_TAG))
                .stream().findFirst().orElseThrow(() -> new AssertionError("Frostwächter ist nicht erschienen"));
    }

    private static void unlock(ClientGameTestContext context, String node) {
        TalentRef ref = new TalentRef(ContentId.of("general"), node);
        context.runOnClient(client -> {
            if (!(client.gui.screen() instanceof TalentTreeScreen screen)) {
                throw new AssertionError("Talentbaum ist nicht offen: " + client.gui.screen());
            }
            screen.selectNode(node);
            screen.unlockSelected();
        });
        context.waitFor(client -> ClientCharacter.sheet().map(s -> s.talents().containsKey(ref)).orElse(false));
    }

    private static CharacterCreationScreen creation(Minecraft client) {
        if (client.gui.screen() instanceof CharacterCreationScreen screen) {
            return screen;
        }
        throw new AssertionError("Charaktererstellung ist nicht offen: " + client.gui.screen());
    }
}
