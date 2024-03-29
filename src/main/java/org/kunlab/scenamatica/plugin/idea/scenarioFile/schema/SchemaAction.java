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
import java.util.Collections;
import java.util.HashMap;
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
            .registerTypeAdapter(ActionIO.class, new ActionIO("", null, new ScenarioType[0], new ScenarioType[0], Collections.emptyMap()))
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

    public record ActionIO(@NotNull String type,
                           @Nullable String description,
                           @Nullable ScenarioType[] requiredOn,
                           @Nullable ScenarioType[] availableFor,
                           @NotNull Map<String, Object> additionalData) implements JsonDeserializer<ActionIO>
    {
        @Override
        public ActionIO deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException
        {
            if (!jsonElement.isJsonObject())
                throw new JsonParseException("ActionArgument must be an object, got " + jsonElement.getClass().getSimpleName());

            JsonObject obj = jsonElement.getAsJsonObject();
            String argumentType = obj.has("type") ? obj.get("type").getAsString(): "any";
            String description = obj.has("description") ? obj.get("description").getAsString(): null;

            ScenarioType[] requiredOn = parseEnumArray(obj.get("requiredOn"), ScenarioType.values(), ActionIO::mapScenarioType)
                    .map(list -> list.toArray(new ScenarioType[0]))
                    .orElse(null);
            ScenarioType[] availableFor = parseEnumArray(obj.get("availableFor"), ScenarioType.values(), ActionIO::mapScenarioType)
                    .map(list -> list.toArray(new ScenarioType[0]))
                    .orElse(null);

            Map<String, Object> additionalData = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet())
            {
                if (entry.getKey().equals("description") || entry.getKey().equals("requiredOn") || entry.getKey().equals("availableFor"))
                    continue;
                additionalData.put(entry.getKey(), entry.getValue());
            }

            return new ActionIO(argumentType, description, requiredOn, availableFor, additionalData);
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

        public <T> Optional<T> getAdditionalData(String key, Class<? extends T> type)
        {
            Object value = this.additionalData.get(key);
            if (value == null)
                return Optional.empty();
            else if (type.isInstance(value))
                return Optional.of(type.cast(value));
            else
                throw new ClassCastException("Cannot cast " + value.getClass().getSimpleName() + " to " + type.getSimpleName());
        }

        public boolean hasAdditionalData(String key)
        {
            return this.additionalData.containsKey(key);
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
