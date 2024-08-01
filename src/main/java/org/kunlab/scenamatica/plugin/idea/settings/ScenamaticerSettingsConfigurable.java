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
import org.kunlab.scenamatica.plugin.idea.ScenamaticerBundle;
import org.kunlab.scenamatica.plugin.idea.ledger.LedgerManagerService;

import java.net.URI;
import java.util.Locale;

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
        this.settings.setOfficialLedgerURL(state.getOfficialLedgerURL());

        this.settings.setRefsWindowAutoOpen(state.isRefsWindowAutoOpen());
        this.settings.setRefsWindowAutoClose(state.isRefsWindowAutoClose());

        this.settings.setScenamaticerLocale(Locale.forLanguageTag(state.getLanguage()));

        return this.settings.getMainPanel();
    }

    @Override
    public boolean isModified()
    {
        ScenamaticerSettingsState state = ScenamaticerSettingsState.getInstance();
        boolean isNotModified = this.settings.getOfficialLedgerURL().equals(state.getOfficialLedgerURL())
                && this.settings.isRefsWindowAutoOpen() == state.isRefsWindowAutoOpen()
                && this.settings.isRefsWindowAutoClose() == state.isRefsWindowAutoClose()
                && this.settings.getScenamaticerLocale().getLanguage().equals(state.getLanguage());

        return !isNotModified;
    }

    @Override
    public void apply() throws ConfigurationException
    {
        ScenamaticerSettingsState state = ScenamaticerSettingsState.getInstance();

        if (!isValidUrl(this.settings.getOfficialLedgerURL()))
            throw new ConfigurationException(String.format(ScenamaticerBundle.of("windows.settings.schema.officialLedgerURL.invalid")));
        state.setOfficialLedgerURL(this.settings.getOfficialLedgerURL());

        state.setRefsWindowAutoOpen(this.settings.isRefsWindowAutoOpen());
        state.setRefsWindowAutoClose(this.settings.isRefsWindowAutoClose());

        String oldLang = state.getLanguage();
        String newLang = this.settings.getScenamaticerLocale().getLanguage();
        if (!oldLang.equals(newLang))
        {
            state.setLanguage(this.settings.getScenamaticerLocale().getLanguage());
            if (ApplicationManager.getApplication().isRestartCapable())
                ApplicationManager.getApplication().restart();
        }


        LedgerManagerService.getInstance().getProvider().setOfficialLedgerURL(state.getOfficialLedgerURL());
        this.reloadSchema();
    }

    private void reloadSchema()
    {
        ApplicationManager.getApplication().runWriteAction(() ->
        {
            for (Project proj : ProjectManager.getInstance().getOpenProjects())
                proj.getService(JsonSchemaService.class).reset();
        });
    }

    private static boolean isValidUrl(String url)
    {
        try
        {
            new URI(url);
            return true;
        }
        catch (Exception e)
        {
            return false;
        }
    }
}
