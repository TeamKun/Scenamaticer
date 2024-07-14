package org.kunlab.scenamatica.plugin.idea.ledger.models;

import lombok.Value;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.models.ScenarioType;

@Value
public class LedgerAdmonition
{
    private static final String KEY_TYPE = "type";
    private static final String KEY_TITLE = "title";
    private static final String KEY_CONTENT = "content";
    private static final String KEY_ON = "on";

    private static final String KEY_SERIALIZED_TYPE = "type";
    private static final String KEY_SERIALIZED_TITLE = "title";
    private static final String KEY_SERIALIZED_CONTENT = "content";
    private static final String KEY_SERIALIZED_ON = "on";

    AdmonitionType type;
    String title;
    String content;
    ScenarioType[] on;

    public LedgerAdmonition()
    {
        this(null, null, null);
    }

    public LedgerAdmonition(AdmonitionType type, String title, String content, ScenarioType... on)
    {
        this.type = type;
        this.title = title;
        this.content = content;
        this.on = on;
    }
}
