package net.frosthain.core.registry;

/** A Minecraft language file ({@code assets/<namespace>/lang/<locale>.json}) of a content pack. */
public record LangFile(String pack, String locale, String location, ContentFile.Opener opener) {
}
