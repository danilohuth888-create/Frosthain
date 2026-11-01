package net.frosthain.core.registry;

import net.frosthain.core.ContentId;
import net.frosthain.core.content.BackgroundDefinition;
import net.frosthain.core.content.ClassDefinition;
import net.frosthain.core.content.ContentDefinition;
import net.frosthain.core.content.PowerDefinition;
import net.frosthain.core.content.ResourceDefinition;
import net.frosthain.core.content.SkillDefinition;
import net.frosthain.core.content.SourceDefinition;
import net.frosthain.core.content.SpeciesDefinition;
import net.frosthain.core.content.SubclassDefinition;
import net.frosthain.core.content.TalentTreeDefinition;
import net.frosthain.core.json.JsonObjectReader;

import java.util.List;
import java.util.function.BiFunction;

/**
 * A kind of content and the folder it lives in:
 * {@code data/<namespace>/frosthain/<folder>/<path>.json} defines {@code <namespace>:<path>}.
 *
 * <p>The same layout is used by the Fabric mod as datapack registry, so content packs can be shipped
 * as ordinary datapacks.
 */
public record ContentType<T extends ContentDefinition>(
        String folder,
        Class<T> definitionClass,
        BiFunction<ContentId, JsonObjectReader, T> parser
) {

    public static final String ROOT_FOLDER = "frosthain";

    public static final ContentType<SourceDefinition> SOURCES =
            new ContentType<>("sources", SourceDefinition.class, SourceDefinition::parse);
    public static final ContentType<SkillDefinition> SKILLS =
            new ContentType<>("skills", SkillDefinition.class, SkillDefinition::parse);
    public static final ContentType<ResourceDefinition> RESOURCES =
            new ContentType<>("resources", ResourceDefinition.class, ResourceDefinition::parse);
    public static final ContentType<PowerDefinition> POWERS =
            new ContentType<>("powers", PowerDefinition.class, PowerDefinition::parse);
    public static final ContentType<SpeciesDefinition> SPECIES =
            new ContentType<>("species", SpeciesDefinition.class, SpeciesDefinition::parse);
    public static final ContentType<ClassDefinition> CLASSES =
            new ContentType<>("classes", ClassDefinition.class, ClassDefinition::parse);
    public static final ContentType<SubclassDefinition> SUBCLASSES =
            new ContentType<>("subclasses", SubclassDefinition.class, SubclassDefinition::parse);
    public static final ContentType<BackgroundDefinition> BACKGROUNDS =
            new ContentType<>("backgrounds", BackgroundDefinition.class, BackgroundDefinition::parse);
    public static final ContentType<TalentTreeDefinition> TALENT_TREES =
            new ContentType<>("talent_trees", TalentTreeDefinition.class, TalentTreeDefinition::parse);

    /** All content types in load order. */
    public static final List<ContentType<?>> ALL = List.of(
            SOURCES, SKILLS, RESOURCES, POWERS, SPECIES, CLASSES, SUBCLASSES, BACKGROUNDS, TALENT_TREES);

    public T parse(ContentId id, JsonObjectReader json) {
        T definition = parser.apply(id, json);
        json.finish();
        return definition;
    }
}
