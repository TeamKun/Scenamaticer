package org.kunlab.scenamatica.plugin.idea.scenarioFile.schema;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.intellij.psi.PsiElement;
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread;
import org.jetbrains.annotations.NotNull;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.lang.tree.ScenarioTrees;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SchemaResolver
{
    private final SchemaProvider provider;

    public SchemaResolver(SchemaProvider provider)
    {
        this.provider = provider;
    }

    @RequiresBackgroundThread
    public String getTypeName(PsiElement element)
    {
        this.provider.initIfNeeded();
        if (element == null)
            return null;

        String absolutePath = ScenarioTrees.getKeyFor(element);
        if (absolutePath == null)
            return null;

        return this.resolveTypeName(absolutePath);
    }

    private String resolveTypeName(@NotNull String absolutePath)
    {
        List<String> parts = new ArrayList<>(Arrays.asList(absolutePath.split("\\.")));
        JsonObject current = this.provider.getPrimeFile();

        String lastType = "prime";
        while (!parts.isEmpty())
        {
            String part = parts.remove(0);

            JsonObject result = processPart(current, part);
            if (result == null)
                return null;

            current = result;
            if (!isPrimitiveType(current))
                lastType = getTypeName(current);
        }

        return lastType;
    }

    private JsonObject processPart(JsonObject current, String part)
    {
        String typeName = getTypeName(current);
        boolean isPrimitiveType = isPrimitiveType(typeName);

        if (isPrimitiveType)
        {
            if (typeName.equals("object"))
                return descendObject(current, part);
            else if (typeName.equals("array"))
                return descendArray(current);
        }
        else if (current.has("properties"))
        {
            JsonObject properties = current.getAsJsonObject("properties");
            if (properties.has(part))
                return properties.getAsJsonObject(part);
        }
        else if (current.has("anyOf"))
        {
            for (JsonElement anyOf : current.getAsJsonArray("anyOf"))
            {
                JsonObject obj = anyOf.getAsJsonObject();
                boolean canApplicable = processPart(obj, part) != null;
                if (canApplicable)
                    return obj;
            }
        }

        if (this.provider.hasDefinition(typeName))
        {
            JsonObject def = this.provider.getDefinitionFile(typeName);
            return processPart(def, part);
        }

        return null;
    }

    private JsonObject descendObject(JsonObject current, String part)
    {
        if (current.has("properties"))
            return current.getAsJsonObject("properties").getAsJsonObject(part);
        else
            return null;
    }

    private JsonObject descendArray(JsonObject current)
    {
        if (current.has("items"))
            return current.getAsJsonObject("items");
        else if (current.has("type") && !isPrimitiveType(current))
            return this.provider.getDefinitionFile(getTypeName(current));
        else
            return null;
    }

    private static boolean isPrimitiveType(JsonObject obj)
    {
        return isPrimitiveType(getTypeName(obj));
    }

    private static String getTypeName(JsonObject obj)
    {
        if (obj != null && obj.has("type"))
            return obj.get("type").getAsString();
        else
            return null;
    }

    private static boolean isPrimitiveType(String type)
    {
        return type != null
                && (type.equals("string")
                || type.equals("number")
                || type.equals("integer")
                || type.equals("boolean")
                || type.equals("null")
                || type.equals("array")
                || type.equals("object"));
    }
}
