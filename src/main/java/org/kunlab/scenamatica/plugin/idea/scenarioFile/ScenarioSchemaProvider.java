package org.kunlab.scenamatica.plugin.idea.scenarioFile;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider;
import com.jetbrains.jsonSchema.extension.SchemaType;
import com.jetbrains.jsonSchema.remote.JsonFileResolver;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.lang.ScenarioFileType;
import org.kunlab.scenamatica.plugin.idea.settings.ScenamaticerSettingsState;

public class ScenarioSchemaProvider implements JsonSchemaFileProvider
{
    private final Project project;
    private VirtualFile schemaFile;
    private boolean remoteDoesntEnabledWarned;

    public ScenarioSchemaProvider(Project project)
    {
        this.project = project;
    }

    @Override
    public boolean isAvailable(@NotNull VirtualFile virtualFile)
    {
        return ScenarioFileType.isType(virtualFile);
    }

    @Override
    public @NotNull @Nls String getName()
    {
        return "Scenamatica Scenario";
    }

    @Override
    public @Nullable VirtualFile getSchemaFile()
    {
        if (this.schemaFile != null && this.schemaFile.isValid())
            return this.schemaFile;

        if (!JsonFileResolver.isRemoteEnabled(this.project))
        {
            if (!this.remoteDoesntEnabledWarned)
            {
                this.remoteDoesntEnabledWarned = true;
                Notifications.Bus.notify(
                        new Notification(
                                "JSON Schema",
                                "Unable to fetch remote schema",
                                "Scenamatica: Remote schema resolution is disabled. Enable it in Settings | Languages & Frameworks | JSON Schema",
                                NotificationType.ERROR
                        )
                );
            }

            return null;
        }

        return this.schemaFile = JsonFileResolver.urlToFile(ScenamaticerSettingsState.getInstance().getSchemaURL());
    }

    @Override
    public boolean isUserVisible()
    {
        return false;
    }

    @Override
    public @Nullable @NonNls String getRemoteSource()
    {
        return ScenamaticerSettingsState.getInstance().getSchemaURL();
    }

    @Override
    public @NotNull SchemaType getSchemaType()
    {
        return SchemaType.remoteSchema;
    }
}
