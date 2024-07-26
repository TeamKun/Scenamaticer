package org.kunlab.scenamatica.plugin.idea.scenarioFile;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider;
import com.jetbrains.jsonSchema.extension.SchemaType;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kunlab.scenamatica.plugin.idea.ledger.LedgerManagerService;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.lang.ScenarioFileType;

import java.nio.file.Path;

public class ScenarioSchemaProvider implements JsonSchemaFileProvider
{
    private final Project project;
    private VirtualFile schemaFile;

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

        LedgerManagerService.getInstance().getProvider().buildCacheAll();

        Path schemaPath = LedgerManagerService.getInstance().getProvider().getOfficialLedger().getCachePath().resolve("json-schema.json");
        return this.schemaFile = VirtualFileManager.getInstance().findFileByNioPath(schemaPath);
    }

    @Override
    public boolean isUserVisible()
    {
        return false;
    }

    @Override
    public @Nullable @NonNls String getRemoteSource()
    {
        return null;
    }

    @Override
    public @NotNull SchemaType getSchemaType()
    {
        return SchemaType.embeddedSchema;
    }
}
