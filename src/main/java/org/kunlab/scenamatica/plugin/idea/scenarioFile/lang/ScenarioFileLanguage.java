package org.kunlab.scenamatica.plugin.idea.scenarioFile.lang;

import com.intellij.lang.Language;
import org.jetbrains.yaml.YAMLLanguage;

public class ScenarioFileLanguage extends Language
{
    public static final ScenarioFileLanguage INSTANCE = new ScenarioFileLanguage();

    public static final String ID = "ScenamaticaScenario";

    private ScenarioFileLanguage()
    {
        super(YAMLLanguage.INSTANCE, ID);
    }
}
