package org.kunlab.scenamatica.plugin.idea.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Data
@State(name = "org.kunlab.scenamatica.plugin.idea.settings.ScenamaticerSettingsState", storages = @Storage("scenamaticer.xml"))
public class ScenamaticerSettingsState implements PersistentStateComponent<ScenamaticerSettingsState>
{
    public static final String DEFAULT_SCHEMA_URL = "https://scenamatica.kunlab.org/schema/scenamatica-file.json";

    private String schemaURL = DEFAULT_SCHEMA_URL;

    @Override
    public @Nullable ScenamaticerSettingsState getState()
    {
        return this;
    }

    @Override
    public void loadState(@NotNull ScenamaticerSettingsState state)
    {
        XmlSerializerUtil.copyBean(state, this);
    }

    public static ScenamaticerSettingsState getInstance()
    {
        return ApplicationManager.getApplication().getService(ScenamaticerSettingsState.class);
    }
}
