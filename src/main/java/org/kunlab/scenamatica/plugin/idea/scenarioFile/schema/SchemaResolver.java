package org.kunlab.scenamatica.plugin.idea.scenarioFile.schema;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread;
import lombok.Data;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLMapping;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.models.ScenarioType;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.tree.ScenarioTrees;
import org.kunlab.scenamatica.plugin.idea.utils.YAMLUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class SchemaResolver
{
    private static final Key<TypeCache> KEY_TYPE_NAME = Key.create("org.kunlab.scenamatica.plugin.idea.scenarioFile.schema.SchemaResolver.TypeName");
    private static final Key<ScenarioAction> KEY_SCENARIO_ACTION = Key.create("org.kunlab.scenamatica.plugin.idea.scenarioFile.schema.SchemaResolver.ScenarioAction");
    private static final Key<Object> KEY_ACTION_SPECFIFIER = Key.create("org.kunlab.scenamatica.plugin.idea.scenarioFile.schema.SchemaResolver.ActionSpecifier");
    private static final Object ACTION_SPECIFIER = new Object();

    private final SchemaProvider provider;

    public SchemaResolver(SchemaProvider provider)
    {
        this.provider = provider;
    }

    public void createCacheAll(PsiElement element)
    {
        ApplicationManager.getApplication().runReadAction(() -> cacheAllRecursive(element));
    }

    private void cacheAllRecursive(PsiElement element)
    {
        if (element == null)
            return;

        for (PsiElement children : element.getChildren())
            cacheAllRecursive(children);

        if (YAMLUtils.isKey(element))
        {
            getTypeName(element);
            getAction(element);
        }
    }

    @RequiresBackgroundThread
    public String getTypeName(PsiElement element)
    {
        this.provider.initIfNeeded();
        if (element == null)
            return null;

        TypeCache cache = element.getUserData(KEY_TYPE_NAME);
        if (cache != null && cache.equalElement(element))
            return cache.getName();

        String typeName = resolveTypeName(element);
        if (typeName != null)
            element.putUserData(KEY_TYPE_NAME, TypeCache.of(element, typeName));

        return typeName;
    }

    public ScenarioAction getAction(PsiElement element)
    {
        this.provider.initIfNeeded();
        if (element == null)
            return null;

        ScenarioAction cache = element.getUserData(KEY_SCENARIO_ACTION);
        if (cache != null && cache.equalElement(element))
            return cache;

        return ApplicationManager.getApplication().runReadAction((Computable<ScenarioAction>) () -> getActionInternal(element));
    }

    private ScenarioAction getActionInternal(PsiElement element)
    {
        YAMLMapping actionElement = findActionByParents(element);
        if (actionElement == null)
            return null;

        YAMLKeyValue action = actionElement.getKeyValueByKey("action");
        if (action == null)
            return null;

        action.putUserData(KEY_ACTION_SPECFIFIER, ACTION_SPECIFIER);
        YAMLKeyValue type = actionElement.getKeyValueByKey("type");

        String actionName = action.getValueText();
        ScenarioType typeEnum = type != null ? ScenarioType.of(type.getValueText()): null;

        YAMLKeyValue argumentsKV = actionElement.getKeyValueByKey("with");
        YAMLMapping arguments = null;
        if (argumentsKV != null && argumentsKV.getValue() instanceof YAMLMapping)
            arguments = (YAMLMapping) argumentsKV.getValue();

        return cacheActionDeeply(actionElement, actionName, typeEnum, arguments);
    }

    public boolean isActionSpecificElement(PsiElement element)
    {
        if (element == null)
            return false;
        else if (element.getUserData(KEY_ACTION_SPECFIFIER) != null)
            return true;

        if (element instanceof LeafPsiElement)
        {
            PsiElement parent = element.getParent();
            if (parent instanceof YAMLKeyValue)
                return isActionSpecificElement(parent);
        }

        return false;
    }

    private YAMLMapping findActionByParents(PsiElement element)
    {
        PsiElement current = element;
        while (current != null)
        {
            String typeName = getTypeName(current);
            if (typeName != null)
                if (typeName.equals("action"))
                {
                    if (element instanceof YAMLKeyValue && ((YAMLKeyValue) element).getKeyText().equals("action"))
                        return (YAMLMapping) element.getParent();
                    else if (element instanceof YAMLMapping)
                        return (YAMLMapping) current;
                }
                else if (typeName.equals("scenario"))
                {
                    YAMLMapping action = resolveActionObjectBy(current);
                    if (action != null)
                        return action;
                }

            current = current.getParent();
        }

        return null;
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
            JsonObject result = null;
            if (lastType != null && lastType.equals("scenario"))
            {
                if (part.equals("with"))
                    // JsonObject を偽装する。アクションの引数は、 properties に噛まされていないため。
                    result = createFakeActionInputJsonObject(current.getAsJsonObject("arguments"));
                else if (part.equals("action"))
                    return "action"; // Secnario 内の action は特別扱い
            }

            if (result == null)
                result = processPart(current, part);

            String typeName = getTypeName(result);
            // Actions のための特殊処理
            if (typeName != null && typeName.equals("scenario"))
            {
                List<String> accessors = parts.subList(0, i + 1);
                result = processScenarioInput(ownerElement, accessors);
            }

            if (result == null)
                return null;

            current = result;
            if (!isPrimitiveType(typeName))
                lastType = typeName;
        }

        return lastType;
    }

    private JsonObject createFakeActionInputJsonObject(JsonObject arguments)
    {
        JsonObject fakeActionInput = new JsonObject();
        fakeActionInput.addProperty("type", "actionArgument");
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

    private static String getActionNameByActionObject(YAMLMapping mapping)
    {
        if (mapping == null)
            return null;

        YAMLKeyValue action = mapping.getKeyValueByKey("action");
        if (action == null)
            return null;

        action.putUserData(KEY_ACTION_SPECFIFIER, ACTION_SPECIFIER);
        return action.getValueText();
    }

    private static ScenarioAction cacheActionDeeply(PsiElement element, String actionName, ScenarioType type, @Nullable YAMLMapping arguments)
    {
        ScenarioAction cache = ScenarioAction.of(element, actionName, type, arguments);
        element.putUserData(KEY_SCENARIO_ACTION, cache);

        Iterator<PsiElement> iterator = new YAMLUtils.DepthFirstIterator(element);
        while (iterator.hasNext())
        {
            PsiElement current = iterator.next();
            current.putUserData(KEY_SCENARIO_ACTION, cache);
        }

        return cache;
    }

    private static String resolveActionNameBy(PsiElement apexElement, List<String> elements)
    {
        return ApplicationManager.getApplication().runReadAction((Computable<String>) () -> {
            YAMLFile yamlFile = (YAMLFile) apexElement.getContainingFile();

            List<String> keys = new ArrayList<>(elements);
            keys.add("action");  // Action name marker

            try
            {
                PsiElement elm = YAMLUtils.getValue(yamlFile, keys.toArray(new String[0]));
                if (elm == null)
                    return null;
                return elm.getText();
            }
            catch (IllegalArgumentException e)
            {
                return null;
            }
        });
    }

    private static YAMLMapping resolveActionObjectBy(PsiElement ownerElement)
    {
        YAMLMapping mapping = null;
        if (ownerElement instanceof YAMLMapping)
            mapping = (YAMLMapping) ownerElement;
        else
        {
            PsiElement current = ownerElement;
            while (current != null)
            {
                if (current instanceof YAMLMapping)
                {
                    mapping = (YAMLMapping) current;
                    break;
                }
                else if (current instanceof YAMLDocument)
                    return null;
                current = current.getParent();
            }

            if (mapping == null)
                return null;
        }

        YAMLKeyValue action = mapping.getKeyValueByKey("action");
        if (action == null)
            return null;
        else
            return mapping;
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

    @Data
    private static class TypeCache
    {
        private final int hash;
        private final String name;

        private TypeCache(int hash, String name)
        {
            this.hash = hash;
            this.name = name;
        }

        public boolean equalElement(PsiElement element)
        {
            return this.hash == calcHash(element);
        }

        public static TypeCache of(PsiElement element, String typeName)
        {
            return new TypeCache(calcHash(element), typeName);
        }

        private static int calcHash(PsiElement element)
        {
            return ApplicationManager.getApplication().runReadAction((Computable<Integer>) () -> {
                Iterator<PsiElement> iterator = new YAMLUtils.DepthFirstIterator(element);
                int hash = element.hashCode();
                while (iterator.hasNext())
                    hash ^= iterator.next().hashCode();

                return hash;
            });
        }
    }

    @Getter
    public static class ScenarioAction extends TypeCache
    {
        private final ScenarioType type;
        @Nullable
        private final YAMLMapping arguments;

        private ScenarioAction(int hash, String typeName, ScenarioType type, @Nullable YAMLMapping arguments)
        {
            super(hash, typeName);
            this.type = type;
            this.arguments = arguments;
        }

        public static ScenarioAction of(PsiElement element, String typeName, ScenarioType type, @Nullable YAMLMapping arguments)
        {
            return new ScenarioAction(TypeCache.calcHash(element), typeName, type, arguments);
        }
    }
}
