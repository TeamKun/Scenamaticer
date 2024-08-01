package org.kunlab.scenamatica.plugin.idea.ledger;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EntityTypeValidator
{
    private static final Map<String, String> ENTITIES;
    private static final Pattern NAMESPACED_KEY_FORMAT = Pattern.compile("([a-zA-Z0-9_]+):([a-zA-Z0-9_]+)");

    static
    {
        Map<String, String> entitiesMap = ENTITIES = new HashMap<>();
        // <editor-fold title="Entities declaration">
        entitiesMap.put("DROPPED_ITEM", "item");
        entitiesMap.put("EXPERIENCE_ORB", "experience_orb");
        entitiesMap.put("AREA_EFFECT_CLOUD", "area_effect_cloud");
        entitiesMap.put("ELDER_GUARDIAN", "elder_guardian");
        entitiesMap.put("WITHER_SKELETON", "wither_skeleton");
        entitiesMap.put("STRAY", "stray");
        entitiesMap.put("EGG", "egg");
        entitiesMap.put("LEASH_HITCH", "leash_knot");
        entitiesMap.put("PAINTING", "painting");
        entitiesMap.put("ARROW", "arrow");
        entitiesMap.put("SNOWBALL", "snowball");
        entitiesMap.put("FIREBALL", "fireball");
        entitiesMap.put("SMALL_FIREBALL", "small_fireball");
        entitiesMap.put("ENDER_PEARL", "ender_pearl");
        entitiesMap.put("ENDER_SIGNAL", "eye_of_ender");
        entitiesMap.put("SPLASH_POTION", "potion");
        entitiesMap.put("THROWN_EXP_BOTTLE", "experience_bottle");
        entitiesMap.put("ITEM_FRAME", "item_frame");
        entitiesMap.put("WITHER_SKULL", "wither_skull");
        entitiesMap.put("PRIMED_TNT", "tnt");
        entitiesMap.put("FALLING_BLOCK", "falling_block");
        entitiesMap.put("FIREWORK", "firework_rocket");
        entitiesMap.put("HUSK", "husk");
        entitiesMap.put("SPECTRAL_ARROW", "spectral_arrow");
        entitiesMap.put("SHULKER_BULLET", "shulker_bullet");
        entitiesMap.put("DRAGON_FIREBALL", "dragon_fireball");
        entitiesMap.put("ZOMBIE_VILLAGER", "zombie_villager");
        entitiesMap.put("SKELETON_HORSE", "skeleton_horse");
        entitiesMap.put("ZOMBIE_HORSE", "zombie_horse");
        entitiesMap.put("ARMOR_STAND", "armor_stand");
        entitiesMap.put("DONKEY", "donkey");
        entitiesMap.put("MULE", "mule");
        entitiesMap.put("EVOKER_FANGS", "evoker_fangs");
        entitiesMap.put("EVOKER", "evoker");
        entitiesMap.put("VEX", "vex");
        entitiesMap.put("VINDICATOR", "vindicator");
        entitiesMap.put("ILLUSIONER", "illusioner");
        entitiesMap.put("MINECART_COMMAND", "command_block_minecart");
        entitiesMap.put("BOAT", "boat");
        entitiesMap.put("MINECART", "minecart");
        entitiesMap.put("MINECART_CHEST", "chest_minecart");
        entitiesMap.put("MINECART_FURNACE", "furnace_minecart");
        entitiesMap.put("MINECART_TNT", "tnt_minecart");
        entitiesMap.put("MINECART_HOPPER", "hopper_minecart");
        entitiesMap.put("MINECART_MOB_SPAWNER", "spawner_minecart");
        entitiesMap.put("CREEPER", "creeper");
        entitiesMap.put("SKELETON", "skeleton");
        entitiesMap.put("SPIDER", "spider");
        entitiesMap.put("GIANT", "giant");
        entitiesMap.put("ZOMBIE", "zombie");
        entitiesMap.put("SLIME", "slime");
        entitiesMap.put("GHAST", "ghast");
        entitiesMap.put("ZOMBIFIED_PIGLIN", "zombified_piglin");
        entitiesMap.put("ENDERMAN", "enderman");
        entitiesMap.put("CAVE_SPIDER", "cave_spider");
        entitiesMap.put("SILVERFISH", "silverfish");
        entitiesMap.put("BLAZE", "blaze");
        entitiesMap.put("MAGMA_CUBE", "magma_cube");
        entitiesMap.put("ENDER_DRAGON", "ender_dragon");
        entitiesMap.put("WITHER", "wither");
        entitiesMap.put("BAT", "bat");
        entitiesMap.put("WITCH", "witch");
        entitiesMap.put("ENDERMITE", "endermite");
        entitiesMap.put("GUARDIAN", "guardian");
        entitiesMap.put("SHULKER", "shulker");
        entitiesMap.put("PIG", "pig");
        entitiesMap.put("SHEEP", "sheep");
        entitiesMap.put("COW", "cow");
        entitiesMap.put("CHICKEN", "chicken");
        entitiesMap.put("SQUID", "squid");
        entitiesMap.put("WOLF", "wolf");
        entitiesMap.put("MUSHROOM_COW", "mooshroom");
        entitiesMap.put("SNOWMAN", "snow_golem");
        entitiesMap.put("OCELOT", "ocelot");
        entitiesMap.put("IRON_GOLEM", "iron_golem");
        entitiesMap.put("HORSE", "horse");
        entitiesMap.put("RABBIT", "rabbit");
        entitiesMap.put("POLAR_BEAR", "polar_bear");
        entitiesMap.put("LLAMA", "llama");
        entitiesMap.put("LLAMA_SPIT", "llama_spit");
        entitiesMap.put("PARROT", "parrot");
        entitiesMap.put("VILLAGER", "villager");
        entitiesMap.put("ENDER_CRYSTAL", "end_crystal");
        entitiesMap.put("TURTLE", "turtle");
        entitiesMap.put("PHANTOM", "phantom");
        entitiesMap.put("TRIDENT", "trident");
        entitiesMap.put("COD", "cod");
        entitiesMap.put("SALMON", "salmon");
        entitiesMap.put("PUFFERFISH", "pufferfish");
        entitiesMap.put("TROPICAL_FISH", "tropical_fish");
        entitiesMap.put("DROWNED", "drowned");
        entitiesMap.put("DOLPHIN", "dolphin");
        entitiesMap.put("CAT", "cat");
        entitiesMap.put("PANDA", "panda");
        entitiesMap.put("PILLAGER", "pillager");
        entitiesMap.put("RAVAGER", "ravager");
        entitiesMap.put("TRADER_LLAMA", "trader_llama");
        entitiesMap.put("WANDERING_TRADER", "wandering_trader");
        entitiesMap.put("FOX", "fox");
        entitiesMap.put("BEE", "bee");
        entitiesMap.put("HOGLIN", "hoglin");
        entitiesMap.put("PIGLIN", "piglin");
        entitiesMap.put("STRIDER", "strider");
        entitiesMap.put("ZOGLIN", "zoglin");
        entitiesMap.put("PIGLIN_BRUTE", "piglin_brute");
        entitiesMap.put("FISHING_HOOK", "fishing_bobber");
        entitiesMap.put("LIGHTNING", "lightning_bolt");
        entitiesMap.put("PLAYER", "player");
        // </editor-fold>
    }

    public static boolean isValidEntityType(@Nullable String entity)
    {
        if (entity == null || entity.isEmpty())
            return false;

        Matcher nsKeyMatcher = NAMESPACED_KEY_FORMAT.matcher(entity);
        if (nsKeyMatcher.matches())
        {
            String namespace = nsKeyMatcher.group(1);
            String value = nsKeyMatcher.group(2);
            if (!namespace.equalsIgnoreCase("minecraft"))
                return false;

            return ENTITIES.values().stream().anyMatch(value::equalsIgnoreCase);
        }

        return ENTITIES.values().stream().anyMatch(entity::equalsIgnoreCase)
                || ENTITIES.keySet().stream().anyMatch(entity::equalsIgnoreCase);
    }
}
