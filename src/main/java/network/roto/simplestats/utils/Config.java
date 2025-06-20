package network.roto.simplestats.utils;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class Config {
    public static final Config CONFIG;
    public static final ModConfigSpec CONFIG_SPEC;
    public final ModConfigSpec.ConfigValue<Integer> baseXP;
    public final ModConfigSpec.ConfigValue<Integer> pointsPerLevel;
    public final ModConfigSpec.ConfigValue<Double> xpScaling;
    public final ModConfigSpec.ConfigValue<Integer> baseLevelRequirement;
    public final ModConfigSpec.ConfigValue<List<? extends String>> perkList;
    public final ModConfigSpec.ConfigValue<List<? extends String>> xpValues;

    public final List<String> defaultPerkList = List.of("test_perk,Test Perk,,3,0,say @p Levelled up!,say @p Levelled Down");
    public final Supplier<String> perkSupplier = () -> "test_perk,Test Perk,,3,0,say @p Levelled up!,say @p Levelled Down";
    public final Predicate<Object> perkPredicator = (Object obj) -> obj instanceof String;

    public final List<String> defaultXpList = List.of("minecraft:wither,10");
    public final Supplier<String> xpSupplier = () -> "minecraft:wither,10";
    public final Predicate<Object> xpPredicator = (Object obj) -> obj instanceof String;

    private Config(ModConfigSpec.Builder builder){
        builder.push("Experience");
        baseXP = builder
                .comment("Base XP dropped by mobs not defined in the list below")
                .define("base_xp", 1);
        xpScaling = builder
                .comment("Rate at which XP scales, Higher values mean more XP needed")
                .define("xp_scaling", 1.5);
        baseLevelRequirement = builder
                .comment("XP required for first level up")
                .define("base_level_requirement", 100);
        pointsPerLevel = builder
                .comment("Points awarded per level")
                .define("points_per_level", 1);
        xpValues = builder
                .comment("List XP values for Mobs, can be empty")
                .comment("In the following format: Mob (string), XP Granted (int). Don't include spaces.")
                .defineListAllowEmpty("xp_values", defaultXpList, xpSupplier, xpPredicator);
        builder.pop();
        builder.push("Perks");
        perkList = builder
                .comment("List of Perks, can be empty")
                .comment("In the following format: ID (string), Name, (string), Icon (String), Max Level (int), Cost per Level (int), Action on Level Up (string), Action on Level Down (string).")
                .comment("All fields are required. Don't include spaces")
                .defineListAllowEmpty("perks", defaultPerkList, perkSupplier, perkPredicator);
        builder.pop();
    }

    static {
        Pair<Config, ModConfigSpec> pair =
            new ModConfigSpec.Builder().configure(Config::new);

        //Store the resulting values
        CONFIG = pair.getLeft();
        CONFIG_SPEC = pair.getRight();
    }
}
