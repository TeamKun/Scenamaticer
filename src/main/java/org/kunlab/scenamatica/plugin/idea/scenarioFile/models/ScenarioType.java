package org.kunlab.scenamatica.plugin.idea.scenarioFile.models;

import lombok.AllArgsConstructor;
import org.kunlab.scenamatica.plugin.idea.ScenamaticerBundle;

import java.util.Locale;

@AllArgsConstructor
public enum ScenarioType
{
    EXECUTE("models.scenarioType.execute"),
    EXPECT("models.scenarioType.expect"),
    REQUIRE("models.scenarioType.require");

    public final String displayName;

    public String getDisplayName()
    {
        return ScenamaticerBundle.of(this.displayName);
    }

    public static ScenarioType of(String name)
    {
        // これ以上は増えないことが確定しているため。
        return switch (name.toLowerCase(Locale.ROOT))
        {
            case "execute" -> EXECUTE;
            case "expect" -> EXPECT;
            case "require" -> REQUIRE;
            default -> null;
        };
    }
}
