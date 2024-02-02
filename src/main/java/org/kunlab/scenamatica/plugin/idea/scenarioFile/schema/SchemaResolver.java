package org.kunlab.scenamatica.plugin.idea.scenarioFile.schema;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiElement;
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread;
import org.jetbrains.yaml.psi.YAMLFile;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.lang.tree.ScenarioTrees;
import org.kunlab.scenamatica.plugin.idea.utils.YAMLUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SchemaResolver
{
    private static final Key<TypeCache> KEY_TYPE_NAME = Key.create("org.kunlab.scenamatica.plugin.idea.scenarioFile.schema.SchemaResolver.TypeName");

    private final SchemaProvider provider;

    public SchemaResolver(SchemaProvider provider)
    {
        this.provider = provider;
    }

    public void createCacheAll(PsiElement element)
    {
        if (element == null)
            return;

        for (PsiElement children : element.getChildren())
            createCacheAll(children);

        if (YAMLUtils.isKey(element))
            getTypeName(element);
    }

    @RequiresBackgroundThread
    public String getTypeName(PsiElement element)
    {
        this.provider.initIfNeeded();
        if (element == null)
            return null;

        TypeCache cache = element.getUserData(KEY_TYPE_NAME);
        if (cache != null && cache.equalElement(element))
            return cache.typeName();

        String typeName = resolveTypeName(element);
        if (typeName != null)
            element.putUserData(KEY_TYPE_NAME, TypeCache.of(element, typeName));

        return typeName;
    }

    private String resolveTypeName(PsiElement ownerElement)
    {
        String absolutePath = ScenarioTrees.getKeyFor(ownerElement);
        if (absolutePath == null)
            return null;

        List<String> parts = new ArrayList<>(Arrays.asList(absolutePath.split("\\.")));
        JsonObject current = this.provider.getPrimeFile();

        String lastType = "prime";
        for (int i = 0; i < parts.size(); i++)
        {
            String part = parts.get(i);
            // Actions のための特殊処理
            JsonObject result;
            if (lastType != null && lastType.equals("scenario") && part.equals("with"))
            {
                assert current != null;
                // JsonObject を偽装する。アクションの引数は、 properties に噛まされていないため。
                result = createFakeActionInputJsonObject(current.getAsJsonObject("arguments"));
            }
            else
                result = processPart(current, part);
            if (result == null)
                return null;

            String typeName = getTypeName(result);
            // Actions のための特殊処理
            if (typeName != null && typeName.equals("scenario"))
            {
                List<String> accessors = parts.subList(0, i + 1);
                result = processScenarioInput(ownerElement, accessors);
            }

            current = result;
            if (!isPrimitiveType(typeName))
                lastType = typeName;
        }

        return lastType;
    }

    private JsonObject createFakeActionInputJsonObject(JsonObject arguments)
    {
        JsonObject fakeActionInput = new JsonObject();
        fakeActionInput.addProperty("type", "object");
        fakeActionInput.add("properties", arguments);
        return fakeActionInput;
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
            if (properties == null)
                return null;
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

    private JsonObject processScenarioInput(PsiElement ownerElement, List<String> elements)
    {
        String resolveActionNameBy = resolveActionNameBy(ownerElement, elements);

        if (this.provider.hasAction(resolveActionNameBy))
            return this.provider.getActionFile(resolveActionNameBy);
        else
            return null;
    }

    private static String resolveActionNameBy(PsiElement ownerElement, List<String> elements)
    {
        return ApplicationManager.getApplication().runReadAction((Computable<String>) () -> {
            YAMLFile yamlFile = (YAMLFile) ownerElement.getContainingFile();

            List<String> keys = new ArrayList<>(elements);
            keys.add("action");  // Action name marker

            try
            {
                PsiElement elm = YAMLUtils.getValue(yamlFile, keys.toArray(new String[0]));
                return elm.getText();
            }
            catch (IllegalArgumentException e)
            {
                return null;
            }
        });
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

    private record TypeCache(int hash, String typeName)
    {
        public boolean equalElement(PsiElement element)
        {
            return element.hashCode() == this.hash;
        }

        public static TypeCache of(PsiElement element, String typeName)
        {
            return new TypeCache(element.hashCode(), typeName);
        }
    }
}
