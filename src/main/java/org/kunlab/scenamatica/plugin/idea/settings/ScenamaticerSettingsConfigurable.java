package org.kunlab.scenamatica.plugin.idea.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.NlsContexts;
import com.jetbrains.jsonSchema.ide.JsonSchemaService;
import javax.swing.JComponent;
import org.jetbrains.annotations.Nullable;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.schema.SchemaProviderService;

import java.net.URL;

public class ScenamaticerSettingsConfigurable implements Configurable
{
    private ScenamaticerSettingsComponent settings;

    @Override
    public @NlsContexts.ConfigurableName String getDisplayName()
    {
        return "Scenamatica";
    }

    @Override
    public @Nullable JComponent createComponent()
    {
        this.settings = new ScenamaticerSettingsComponent();

        ScenamaticerSettingsState state = ScenamaticerSettingsState.getInstance();
        this.settings.setSchemaURL(state.getSchemaURL());
        this.settings.setContentServerURL(state.getContentServerURL());

        this.settings.setRefsWindowEnabled(state.isRefsNavigationEnabled());
        this.settings.setRefsWindowAutoOpen(state.isRefsWindowAutoOpen());
        this.settings.setRefsWindowAutoClose(state.isRefsWindowAutoClose());

        return this.settings.getMainPanel();
    }

    @Override
    public boolean isModified()
    {
        ScenamaticerSettingsState state = ScenamaticerSettingsState.getInstance();
        boolean isNotModified = this.settings.getSchemaURL().equals(state.getSchemaURL())
                && this.settings.getContentServerURL().equals(state.getContentServerURL())
                && this.settings.isRefsNavigationEnabled() == state.isRefsNavigationEnabled()
                && this.settings.isRefsWindowAutoOpen() == state.isRefsWindowAutoOpen()
                && this.settings.isRefsWindowAutoClose() == state.isRefsWindowAutoClose();

        return !isNotModified;
    }

    @Override
    public void apply() throws ConfigurationException
    {
        ScenamaticerSettingsState state = ScenamaticerSettingsState.getInstance();

        if (!isValidUrl(this.settings.getSchemaURL()))
            throw new ConfigurationException("Invalid URL provided for schema");
        state.setSchemaURL(this.settings.getSchemaURL());
        if (!isValidUrl(state.getSchemaURL()))
            throw new ConfigurationException("Invalid URL provided for scenamatica contents");
        state.setContentServerURL(this.settings.getContentServerURL());

        state.setRefsNavigationEnabled(this.settings.isRefsNavigationEnabled());
        state.setRefsWindowAutoOpen(this.settings.isRefsWindowAutoOpen());
        state.setRefsWindowAutoClose(this.settings.isRefsWindowAutoClose());

        SchemaProviderService.getInstance().getSchemaProvider().setContentServerURL(state.getContentServerURL());
        this.reloadSchema();
    }

    private void reloadSchema()
    {
        ApplicationManager.getApplication().runWriteAction(() ->
        {
            for (Project proj : ProjectManager.getInstance().getOpenProjects())
            {
                proj.getService(JsonSchemaService.class).reset();
            }
        });
    }

    private static boolean isValidUrl(String url)
    {
        try
        {
            new URL(url);
            return true;
        }
        catch (Exception e)
        {
            return false;
        }
    }
}
