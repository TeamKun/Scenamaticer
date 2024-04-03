package org.kunlab.scenamatica.plugin.idea.scenarioFile.policy.lang;

import com.intellij.lang.Language;
import org.jetbrains.yaml.YAMLLanguage;

public class ScenamaticaPolicyLanguage extends Language
{
    public static final ScenamaticaPolicyLanguage INSTANCE = new ScenamaticaPolicyLanguage();

    public static final String ID = "ScenamaticaPolicy";

    private ScenamaticaPolicyLanguage()
    {
        super(YAMLLanguage.INSTANCE, ID);
    }


}
