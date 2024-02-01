package org.kunlab.scenamatica.plugin.idea.scenarioFile.lang;

import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiElement;
import org.jetbrains.yaml.psi.YAMLValue;
import org.jetbrains.yaml.psi.impl.YAMLBlockMappingImpl;
import org.kunlab.scenamatica.plugin.idea.utils.YAMLUtils;

import java.util.Objects;

public class ScenarioTrees
{
    public static final Key<String> KEY_YAML_KEY = Key.create("org.kunlab.scenamatica.plugin.idea.scenarioFile.lang.ScenarioTrees.YamlKEY");

    public static void embedKeyAll(PsiElement element)
    {
        if (element.getParent() == null || (element.getParent() instanceof YAMLBlockMappingImpl && element instanceof YAMLValue))
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
}
