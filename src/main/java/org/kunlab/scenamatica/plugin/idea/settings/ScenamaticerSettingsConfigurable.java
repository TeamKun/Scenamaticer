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

        return this.settings.getMainPanel();
    }

    @Override
    public boolean isModified()
    {
        ScenamaticerSettingsState state = ScenamaticerSettingsState.getInstance();
        boolean isNotModified = this.settings.getSchemaURL().equals(state.getSchemaURL())
                || this.settings.isRefsWindowEnabled() == state.isRefsWindowEnabled()
                || this.settings.isRefsWindowAutoOpen() == state.isRefsWindowAutoOpen()
                || this.settings.isRefsWindowAutoClose() == state.isRefsWindowAutoClose();

        return !isNotModified;
    }

    @Override
    public void apply() throws ConfigurationException
    {
        ScenamaticerSettingsState state = ScenamaticerSettingsState.getInstance();

        if (!isValidUrl(this.settings.getSchemaURL()))
            throw new ConfigurationException("Invalid URL provided");
        state.setSchemaURL(this.settings.getSchemaURL());

        state.setRefsWindowEnabled(this.settings.isRefsWindowEnabled());
        state.setRefsWindowAutoOpen(this.settings.isRefsWindowAutoOpen());
        state.setRefsWindowAutoClose(this.settings.isRefsWindowAutoClose());

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
