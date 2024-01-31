package org.kunlab.scenamatica.plugin.idea.scenarioFile.schema;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import lombok.Getter;
import org.kunlab.scenamatica.plugin.idea.settings.ScenamaticerSettingsState;

@Getter
@Service(Service.Level.APP)
public final class SchemaProviderService
{
    private final SchemaProvider schemaProvider;

    public SchemaProviderService()
    {
        this.schemaProvider = new SchemaProvider(
                ScenamaticerSettingsState.getInstance().getContentServerURL()
        );
    }

    public static SchemaProviderService getInstance()
    {
        return ApplicationManager.getApplication().getService(SchemaProviderService.class);
    }
}
