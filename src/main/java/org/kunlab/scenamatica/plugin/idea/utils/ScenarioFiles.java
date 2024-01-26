package org.kunlab.scenamatica.plugin.idea.utils;

import org.jetbrains.yaml.YAMLUtil;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;

public class ScenarioFiles
{
    public static final String KEY_SCENAMATICA = "scenamatica";

    public static boolean isScenarioFile(YAMLFile yaml)
    {
        for (YAMLKeyValue keyValue : YAMLUtil.getTopLevelKeys(yaml))
        {
            if (keyValue.getKeyText().equals(KEY_SCENAMATICA))
                return true;
        }

        return false;
    }
}
