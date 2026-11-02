package net.frosthain.mod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.frosthain.core.registry.ContentLoader;
import net.frosthain.core.registry.ContentRegistry;
import net.frosthain.core.registry.ContentType;
import net.frosthain.core.registry.DirectoryContentSource;
import net.frosthain.core.validation.ContentValidator;
import net.frosthain.core.validation.Diagnostic;
import net.frosthain.mod.character.CharacterService;
import net.frosthain.mod.character.DialogueService;
import net.frosthain.mod.character.QuestService;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Entry point of the FROSTHAIN mod (milestone M0.3: skeleton).
 *
 * <p>Loads the bundled base content through the Minecraft-independent core and logs the result of the
 * validation. Data pack reloading, player data and screens follow in the next steps
 * (docs/10-ui-implementierungsplan.md, U4/U5).
 */
public final class FrosthainMod implements ModInitializer {

    public static final String MOD_ID = "frosthain";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static volatile ContentRegistry content;

    @Override
    public void onInitialize() {
        ModContainer mod = FabricLoader.getInstance().getModContainer(MOD_ID)
                .orElseThrow(() -> new IllegalStateException("FROSTHAIN-Mod-Container fehlt"));
        DirectoryContentSource.Scan scan = DirectoryContentSource.scanPacks(mod.getRootPaths());
        ContentLoader.Result result = ContentLoader.load(scan.contentFiles(), scan.langFiles());
        List<Diagnostic> diagnostics = new ArrayList<>(result.diagnostics());
        diagnostics.addAll(ContentValidator.validate(result.registry()));
        long errors = diagnostics.stream().filter(d -> d.severity() == Diagnostic.Severity.ERROR).count();
        for (Diagnostic diagnostic : diagnostics) {
            switch (diagnostic.severity()) {
                case ERROR -> LOGGER.error("{}", diagnostic);
                case WARNING -> LOGGER.warn("{}", diagnostic);
                case INFO -> LOGGER.debug("{}", diagnostic);
            }
        }
        content = result.registry();
        CharacterService.register();
        QuestService.register();
        DialogueService.register();
        LOGGER.info("FROSTHAIN geladen: {} Klassen, {} Spezies, {} Talentbäume, {} Quests, {} Dialoge – {} Fehler",
                content.all(ContentType.CLASSES).size(), content.all(ContentType.SPECIES).size(),
                content.all(ContentType.TALENT_TREES).size(), content.all(ContentType.QUESTS).size(),
                content.all(ContentType.DIALOGUES).size(), errors);
    }

    /** Content loaded at start-up; replaced by the data pack registry in the next step. */
    public static ContentRegistry content() {
        return content;
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
