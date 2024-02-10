package org.kunlab.scenamatica.plugin.idea.scenarioFile.schema;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread;
import kotlin.Pair;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.models.ScenarioType;
import org.kunlab.scenamatica.plugin.idea.utils.JsonUtils;
import org.kunlab.scenamatica.plugin.idea.utils.TempFileDownloader;
import org.kunlab.scenamatica.plugin.idea.utils.URLUtils;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class SchemaProvider
{
    public static final String PATH_FILE_META = "meta.json";
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(AvailableStatus.class, new AvailableStatus(false, null))
            .create();

    @Getter
    private final SchemaResolver schemaResolver;
    private final Map<String, Pair<JsonObject, Action>> actionsCache;
    private final Map<String, JsonObject> definitionCache;

    private String contentServerURL;

    @Getter
    private VirtualFile metaFile;
    @Getter
    private SchemaMeta meta;

    @Getter
    private JsonObject primeFile;

    private boolean initialized;

    public SchemaProvider(String contentServerURL)
    {
        this.contentServerURL = contentServerURL;
        this.schemaResolver = new SchemaResolver(this);
        this.actionsCache = new HashMap<>();
        this.definitionCache = new HashMap<>();

        this.setContentServerURL(contentServerURL);
    }

    public void setContentServerURL(String contentServerURL)
    {
        this.contentServerURL = contentServerURL;
        this.initialized = false;
    }

    public Action getAction(String action)
    {
        checkMetaLoaded();

        Pair<JsonObject, Action> pair = getActionJsonRecursive(action);
        return pair == null ? null: pair.getSecond();
    }

    public JsonObject getActionFile(String action)
    {
        checkMetaLoaded();

        Pair<JsonObject, Action> pair = getActionJsonRecursive(action);
        return pair == null ? null: pair.getFirst();
    }

    private Pair<JsonObject, Action> getActionJsonRecursive(String action)
    {
        if (this.actionsCache.containsKey(action))
            return this.actionsCache.get(action);

        if (!this.meta.isActionExists(action))
            return null;

        String actionGroup = this.meta.getActionGroupOf(action);
        SchemaMeta.ActionDescriptor actionDescriptorMeta = this.meta.getAction(actionGroup, action);
        String path = buildActionFilePath(actionGroup, actionDescriptorMeta.getFile());
        JsonObject file = TempFileDownloader.downloadJsonSync(path);
        if (hasBaseActionInAction(file))
        {
            String baseAction = file.get("base").getAsString();

            Pair<JsonObject, Action> basePair = getActionJsonRecursive(baseAction);
            if (basePair == null)
                throw new IllegalStateException("Base action '" + baseAction + "' does not exist");
            file = JsonUtils.mergeRecursive(basePair.getFirst(), file);
        }

        Action actionObj = GSON.fromJson(file, Action.class);
        Pair<JsonObject, Action> pair = new Pair<>(file, actionObj);

        this.actionsCache.put(action, pair);
        return pair;
    }

    public boolean hasDefinition(String definition)
    {
        checkMetaLoaded();
        return this.meta.isDefinitionExists(definition);
    }

    public boolean hasAction(String action)
    {
        checkMetaLoaded();
        return this.meta.isActionExists(action);
    }

    public JsonObject getDefinitionFile(String definition)
    {
        checkMetaLoaded();

        if (!this.meta.isDefinitionExists(definition))
            throw new IllegalStateException("Definition '" + definition + "' does not exist");

        String definitionGroup = this.meta.getDefinitionGroupOf(definition);
        if (this.definitionCache.containsKey(definitionGroup))
            return findDefinition(this.definitionCache.get(definitionGroup), definition);

        String path = buildDefinitionFilePath(definitionGroup);
        JsonObject file = TempFileDownloader.downloadJsonSync(path);

        this.definitionCache.put(definitionGroup, file);
        return findDefinition(file, definition);
    }

    private String buildDefinitionFilePath(String definitionGroup)
    {
        return URLUtils.concat(this.contentServerURL, this.meta.getDefinitionsDir(), definitionGroup + ".json");
    }

    private void checkMetaLoaded()
    {
        if (this.meta == null)
            throw new IllegalStateException("Meta file is not loaded yet");
    }

    private String buildActionFilePath(String group, String action)
    {
        return URLUtils.concat(this.contentServerURL, this.meta.getActionsDir(), group, action);
    }

    @RequiresBackgroundThread
    public void initIfNeeded()
    {
        if (this.initialized)
            return;
        this.metaFile = TempFileDownloader.downloadSync(getContentURL(this.contentServerURL, PATH_FILE_META));
        this.meta = SchemaMeta.fromJSON(this.metaFile);
        this.primeFile = TempFileDownloader.downloadJsonSync(getContentURL(this.contentServerURL, this.meta.getPrime()));
        this.initialized = true;
    }

    private static boolean hasBaseActionInAction(JsonObject file)
    {
        return file.has("base");
    }

    private static JsonObject findDefinition(JsonObject obj, String definition)
    {
        if (obj.has(definition))
            return obj.getAsJsonObject(definition);
        else
            throw new IllegalStateException("Definition '" + definition + "' does not exist in file");
    }

    private static String getContentURL(String contentServerURL, String path)
    {
        return URLUtils.concat(contentServerURL, path);
    }

    public record Action(String name, String description, String base, String[] events, AvailableStatus executable,
                         AvailableStatus watchable, AvailableStatus requireable, Map<String, Object> arguments,
                         Map<String, Object> outputs)
    {
        public boolean isAvailableFor(@Nullable ScenarioType type)
        {
            if (type == null)
                return this.executable.isAvailable()
                        || this.watchable.isAvailable()
                        || this.requireable.isAvailable();

            switch (type)
            {
                case EXECUTE ->
                {
                    return this.executable.isAvailable();
                }
                case EXPECT ->
                {
                    return this.watchable.isAvailable();
                }
                case REQUIRE ->
                {
                    return this.requireable.isAvailable();
                }
                default -> throw new IllegalStateException("Unexpected value: " + type);
            }
        }

        public String getDescriptionFor(@Nullable ScenarioType type)
        {
            if (type == null)
                return this.description();

            switch (type)
            {
                case EXECUTE ->
                {
                    return this.executable.description() == null ? this.description(): this.executable.description();
                }
                case EXPECT ->
                {
                    return this.watchable.description() == null ? this.description(): this.watchable.description();
                }
                case REQUIRE ->
                {
                    return this.requireable.description() == null ? this.description(): this.requireable.description();
                }
                default -> throw new IllegalStateException("Unexpected value: " + type);
            }
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
