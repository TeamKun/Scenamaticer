package org.kunlab.scenamatica.plugin.idea.scenarioFile.lang.tree;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiElement;
import org.jetbrains.yaml.psi.YAMLValue;
import org.jetbrains.yaml.psi.impl.YAMLBlockMappingImpl;
import org.kunlab.scenamatica.plugin.idea.utils.YAMLUtils;

import java.util.Iterator;
import java.util.Objects;

public class ScenarioTrees
{
    public static final Key<String> KEY_YAML_KEY = Key.create("org.kunlab.scenamatica.plugin.idea.scenarioFile.lang.tree.ScenarioTrees.YamlKEY");

    public static void embedKeyAll(PsiElement root)
    {
        if (!canHasKey(root))
            return;

        Iterator<PsiElement> iterator = new YAMLUtils.DepthFirstIterator(root);

        while (iterator.hasNext())
        {
            PsiElement currentElement = iterator.next();

            if (!canHasKey(currentElement))
                continue;

            String key = YAMLUtils.getAbsoluteKeyText(currentElement);
            String knownKey = currentElement.getUserData(KEY_YAML_KEY);
            if (Objects.equals(key, knownKey))
                continue;

            if (YAMLUtils.isValue(currentElement))
            {
                currentElement.putUserData(KEY_YAML_KEY, key);
                currentElement = currentElement.getParent(); // => KV
            }

            if (YAMLUtils.isKeyValue(currentElement))
            {
                currentElement.putUserData(KEY_YAML_KEY, key);
                currentElement = currentElement.getFirstChild(); // => KEY
            }

            if (YAMLUtils.isKey(currentElement))
                currentElement.putUserData(KEY_YAML_KEY, key);
        }
    }

    private static boolean canHasKey(PsiElement element)
    {
        PsiElement parent;
        if (ApplicationManager.getApplication().isReadAccessAllowed())
            parent = element.getParent();
        else
            parent = ApplicationManager.getApplication().runReadAction((Computable<PsiElement>) element::getParent);

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

