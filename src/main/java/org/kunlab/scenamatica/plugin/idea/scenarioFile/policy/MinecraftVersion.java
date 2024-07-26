package org.kunlab.scenamatica.plugin.idea.scenarioFile.policy;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;

@Getter
public enum MinecraftVersion
{
    ANY("any"),

    V1_13_X("1.13.x", "1.13.2"),
    V1_13("1.13"),
    V1_13_1("1.13.1"),
    V1_13_2("1.13.2"),

    V1_14_X("1.14.x", "1.14.4"),
    V1_14("1.14"),
    V1_14_1("1.14.1"),
    V1_14_2("1.14.2"),
    V1_14_3("1.14.3"),
    V1_14_4("1.14.4"),

    V1_15_X("1.15.x", "1.15.2"),
    V1_15("1.15"),
    V1_15_1("1.15.1"),
    V1_15_2("1.15.2"),

    V1_16_X("1.16.x", "1.16.5"),
    V1_16("1.16"),
    V1_16_1("1.16.1"),
    V1_16_2("1.16.2"),
    V1_16_3("1.16.3"),
    V1_16_4("1.16.4"),
    V1_16_5("1.16.5"),

    V1_17_X("1.17.x", "1.17.1"),
    V1_17("1.17"),
    V1_17_1("1.17.1"),

    V1_18_X("1.18.x", "1.18.1"),
    V1_18("1.18"),
    V1_18_1("1.18.1"),
    V1_18_2("1.18.2"),

    V1_19_X("1.19.x", "1.19.4"),
    V1_19("1.19"),
    V1_19_1("1.19.1"),
    V1_19_2("1.19.2"),
    V1_19_3("1.19.3"),
    V1_19_4("1.19.4"),

    V1_20_X("1.20.x", "1.20.4"),
    V1_20("1.20"),
    V1_20_1("1.20.1"),
    V1_20_2("1.20.2"),
    V1_20_3("1.20.3"),
    V1_20_4("1.20.4");

    private final String version;
    private final String rangeEnd;

    MinecraftVersion(String version)
    {
        this.version = version;
        this.rangeEnd = null;
    }

    MinecraftVersion(String version, String rangeEnd)
    {
        this.version = version;
        this.rangeEnd = rangeEnd;
    }

    public boolean isInRange(@Nullable MinecraftVersion min, @Nullable MinecraftVersion max)
    {
        if (this == ANY)
            return true;

        if (min == null)
            min = ANY;
        if (max == null)
            max = ANY;

        MinecraftVersion minRangeEnd = min != ANY && min.rangeEnd != null ? MinecraftVersion.fromString(min.rangeEnd): null;
        MinecraftVersion endRangeEnd = this.rangeEnd != null ? MinecraftVersion.fromString(this.rangeEnd): null;

        return (min == ANY
                || min.compareTo(this) <= 0
                || (minRangeEnd != null && minRangeEnd.compareTo(this) >= 0))
                && (max == ANY
                || max.compareTo(this) >= 0
                || (endRangeEnd != null && endRangeEnd.compareTo(this) <= 0));
    }

    @Override
    public String toString()
    {
        return this.version;
    }

    public static boolean isVersion(String version)
    {
        return fromString(version) != ANY;
    }

    @NotNull
    public static MinecraftVersion fromString(@Nullable String version)
    {
        for (MinecraftVersion v : values())
        {
            if (version == null)
                break;

            if (v.getVersion().equals(version))
                return v;
        }
        return ANY;
    }

    public static class Deserializer implements JsonDeserializer<MinecraftVersion>
    {
        @Override
        public MinecraftVersion deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
        {
            return MinecraftVersion.fromString(json.getAsString());
        }
    }
}
