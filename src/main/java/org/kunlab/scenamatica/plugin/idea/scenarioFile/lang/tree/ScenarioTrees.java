package org.kunlab.scenamatica.plugin.idea.scenarioFile.lang.tree;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiElement;
import org.jetbrains.yaml.psi.YAMLValue;
import org.jetbrains.yaml.psi.impl.YAMLBlockMappingImpl;
import org.kunlab.scenamatica.plugin.idea.utils.YAMLUtils;

import java.util.Objects;

public class ScenarioTrees
{
    public static final Key<String> KEY_YAML_KEY = Key.create("org.kunlab.scenamatica.plugin.idea.scenarioFile.lang.tree.ScenarioTrees.YamlKEY");

    public static void embedKeyAll(PsiElement element)
    {
        if (!canHasKey(element))
            return;

        String key = YAMLUtils.getAbsoluteKeyText(element);
        String knownKey = element.getUserData(KEY_YAML_KEY);
        if (Objects.equals(key, knownKey))
            return;

        for (PsiElement child : element.getChildren())
            embedKeyAll(child);

        if (YAMLUtils.isValue(element))
        {
            element.putUserData(KEY_YAML_KEY, key);
            element = element.getParent(); // => KV
        }

        if (YAMLUtils.isKeyValue(element))
        {
            element.putUserData(KEY_YAML_KEY, key);
            element = element.getFirstChild(); // => KEY
        }

        if (YAMLUtils.isKey(element))
            element.putUserData(KEY_YAML_KEY, key);
    }

    private static boolean canHasKey(PsiElement element)
    {
        PsiElement parent = ApplicationManager.getApplication().runReadAction((Computable<? extends PsiElement>) element::getParent);

        return (parent instanceof YAMLBlockMappingImpl && element instanceof YAMLValue)
                || YAMLUtils.isValue(element)
                || YAMLUtils.isKeyValue(element)
                || YAMLUtils.isKey(element);
    }

    public static String getKeyFor(PsiElement element)
    {
        String result = element.getUserData(KEY_YAML_KEY);
        if (result == null && canHasKey(element))
        {
            embedKeyAll(element);
            result = element.getUserData(KEY_YAML_KEY);
        }

        return result;
    }
}

