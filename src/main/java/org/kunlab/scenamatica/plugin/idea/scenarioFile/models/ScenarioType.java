package org.kunlab.scenamatica.plugin.idea.scenarioFile.models;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Locale;

@Getter
@AllArgsConstructor
public enum ScenarioType
{
    EXECUTE("execute"),
    EXPECT("expect"),
    REQUIRE("require");

    public final String name;

    public static org.kunlab.scenamatica.plugin.idea.scenarioFile.models.ScenarioType of(String name)
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
