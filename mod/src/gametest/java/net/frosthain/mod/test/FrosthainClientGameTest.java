package net.frosthain.mod.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.frosthain.core.registry.ContentType;
import net.frosthain.mod.FrosthainMod;

/**
 * Real client start test: boots the Minecraft client with FROSTHAIN, creates a singleplayer world,
 * waits until chunks are rendered and takes screenshots. Runs headless in CI (xvfb + Mesa).
 */
public final class FrosthainClientGameTest implements FabricClientGameTest {

    @Override
    public void runTest(ClientGameTestContext context) {
        context.takeScreenshot("frosthain-01-titelbildschirm");
        if (FrosthainMod.content() == null || FrosthainMod.content().all(ContentType.CLASSES).isEmpty()) {
            throw new AssertionError("FROSTHAIN-Inhalte wurden im Client nicht geladen");
        }
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            singleplayer.getClientWorld().waitForChunksRender();
            context.takeScreenshot("frosthain-02-einzelspielerwelt");
        }
    }
}
