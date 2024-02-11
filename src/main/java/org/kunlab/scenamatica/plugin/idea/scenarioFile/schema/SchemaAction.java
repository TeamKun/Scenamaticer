package org.kunlab.scenamatica.plugin.idea.scenarioFile.schema;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.models.ScenarioType;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public record SchemaAction(String name, String description, String base, String[] events,
                           AvailableStatus executable,
                           AvailableStatus watchable, AvailableStatus requireable,
                           Map<String, ActionIO> arguments,
                           Map<String, ActionIO> outputs)
{
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(AvailableStatus.class, new AvailableStatus(false, null))
            .registerTypeAdapter(ActionIO.class, new ActionIO(null, new ScenarioType[0], new ScenarioType[0]))
            .create();

    public boolean isAvailableFor(@Nullable ScenarioType type)
    {
        if (type == null)
            return this.executable.isAvailable()
                    || this.watchable.isAvailable()
                    || this.requireable.isAvailable();

        return switch (type)
        {
            case EXECUTE -> this.executable.isAvailable();
            case EXPECT -> this.watchable.isAvailable();
            case REQUIRE -> this.requireable.isAvailable();
        };
    }

    public String getDescriptionFor(@Nullable ScenarioType type)
    {
        if (type == null)
            return this.description();

        return switch (type)
        {
            case EXECUTE -> getOrDefault(this.executable.description(), this.description());
            case EXPECT -> getOrDefault(this.watchable.description(), this.description());
            case REQUIRE -> getOrDefault(this.requireable.description(), this.description());
        };
    }

    private static String getOrDefault(String str, String def)
    {
        return str == null || str.isBlank() ? def: str;
    }

    public static SchemaAction fromJson(@NotNull JsonObject obj)
    {
        return GSON.fromJson(obj, SchemaAction.class);
    }

    public record ActionIO(@Nullable String description,
                           @Nullable ScenarioType[] requiredOn,
                           @Nullable ScenarioType[] availableFor) implements JsonDeserializer<ActionIO>
    {
        @Override
        public ActionIO deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException
        {
            if (!jsonElement.isJsonObject())
                throw new JsonParseException("ActionArgument must be an object, got " + jsonElement.getClass().getSimpleName());

            JsonObject obj = jsonElement.getAsJsonObject();
            String description = obj.has("description") ? obj.get("description").getAsString(): null;

            ScenarioType[] requiredOn = parseEnumArray(obj.get("requiredOn"), ScenarioType.values(), ActionIO::mapScenarioType)
                    .map(list -> list.toArray(new ScenarioType[0]))
                    .orElse(null);
            ScenarioType[] availableFor = parseEnumArray(obj.get("availableFor"), ScenarioType.values(), ActionIO::mapScenarioType)
                    .map(list -> list.toArray(new ScenarioType[0]))
                    .orElse(null);

            return new ActionIO(description, requiredOn, availableFor);
        }

        public boolean isAvailableFor(ScenarioType type)
        {
            if (this.availableFor == null)
                return true;

            for (ScenarioType t : this.availableFor)
                if (t == type)
                    return true;
            return false;
        }

        public boolean isRequiredOn(ScenarioType type)
        {
            if (this.requiredOn == null)
                return false;

            for (ScenarioType t : this.requiredOn)
                if (t == type)
                    return true;
            return false;
        }

        private static ScenarioType mapScenarioType(String str)
        {
            if (str == null)
                return null;
            else if (str.equalsIgnoreCase("watch"))
                return ScenarioType.EXPECT;
            return ScenarioType.of(str);
        }

        private static <T extends Enum<T>> Optional<? extends List<T>> parseEnumArray(JsonElement element, T[] allValues, Function<? super String, T> mapper)
        {
            if (element == null)
                return Optional.empty();
            else if (element.isJsonArray())
            {
                return Optional.of(element.getAsJsonArray().asList().stream()
                        .map(JsonElement::getAsString)
                        .map(mapper)
                        .toList());
            }
            else if (element.isJsonPrimitive())
            {
                JsonPrimitive primitive = element.getAsJsonPrimitive();
                if (!primitive.isBoolean())
                    throw new JsonParseException("Must be a boolean or an array of strings, got " + primitive.getClass().getSimpleName());

                if (primitive.getAsBoolean())
                    return Optional.of(List.of(allValues));
                else
                    return Optional.of(List.of());
            }

            throw new JsonParseException("Must be a boolean or an array of strings, got " + element.getClass().getSimpleName());
        }
    }

    public record AvailableStatus(boolean isAvailable, String description) implements JsonDeserializer<AvailableStatus>
    {
        @Override
        public AvailableStatus deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException
        {
            if (!jsonElement.isJsonPrimitive())
                throw new JsonParseException("AvailableStatus must be a primitive type, got " + jsonElement.getClass().getSimpleName());

            JsonPrimitive primitive = jsonElement.getAsJsonPrimitive();
            if (primitive.isBoolean())
                return new AvailableStatus(primitive.getAsBoolean(), null);
            else if (primitive.isString())
                return new AvailableStatus(true, primitive.getAsString());
            else
                throw new JsonParseException("AvailableStatus must be a primitive type, got " + primitive.getClass().getSimpleName());
        }
    }
}
