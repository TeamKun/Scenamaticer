package org.kunlab.scenamatica.plugin.idea.settings;

import com.intellij.DynamicBundle;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import lombok.Getter;
import org.kunlab.scenamatica.plugin.idea.ScenamaticerBundle;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.schema.SchemaProviderService;

import java.awt.event.ActionEvent;
import java.util.Locale;

public class ScenamaticerSettingsComponent
{
    @Getter
    private JPanel mainPanel;
    private JBTextField schemaURL;
    private JTextField contentServerURL;

    private JBCheckBox ckbRefsWindowAutoOpen;
    private JBCheckBox ckbRefsWindowAutoClose;
    private JComboBox<String> scenamaticerLocale;
    private JBLabel lbLanguageSettings;
    private JBLabel lbChangesWillBeAppliedAfterIDERestart;
    private JBLabel lbScenarioSchemaSettings;
    private JBLabel lbScenamaticaSettings;
    private JBLabel lbJsonSchemaURL;
    private JLabel lbScenamaticaContentServerURL;
    private JBLabel lbReferencesWindowSettings;
    private JButton btnPurgeCache;

    public ScenamaticerSettingsComponent()
    {
        this.initComponents();

        this.btnPurgeCache.addActionListener(this::onPurgeCache);
    }

    private void onPurgeCache(ActionEvent actionEvent)
    {
        SchemaProviderService.getProvider().clearCache();
        NotificationGroupManager.getInstance()
                .getNotificationGroup("Scenamatica")
                .createNotification(
                        "Cache purged",
                        NotificationType.WARNING
                )
                .notify(null);
    }

    private void initComponents()
    {
        ScenamaticerBundle.embed(this.lbScenamaticaSettings, "windows.settings.title");
        ScenamaticerBundle.embed(this.lbScenarioSchemaSettings, "windows.settings.schema.title");
        ScenamaticerBundle.embed(this.lbJsonSchemaURL, "windows.settings.schema.jsonSchemaURL");
        ScenamaticerBundle.embed(this.lbScenamaticaContentServerURL, "windows.settings.schema.contentServerURL");
        ScenamaticerBundle.embed(this.lbReferencesWindowSettings, "windows.settings.references.title");
        ScenamaticerBundle.embed(this.ckbRefsWindowAutoOpen, "windows.settings.references.autoOpenOnFileOpen");
        ScenamaticerBundle.embed(this.ckbRefsWindowAutoClose, "windows.settings.references.autoCloseOnFileClose");
        ScenamaticerBundle.embed(this.lbLanguageSettings, "windows.settings.language.title");
        ScenamaticerBundle.embed(this.lbChangesWillBeAppliedAfterIDERestart, "windows.settings.changesWillBeAppliedAfterRestart");
        ScenamaticerBundle.embed(this.btnPurgeCache, "windows.settings.purgeCache");
    }

    public String getSchemaURL()
    {
        return this.schemaURL.getText();
    }

    public void setSchemaURL(String schemaURL)
    {
        this.schemaURL.setText(schemaURL);
    }

    public String getContentServerURL()
    {
        return this.contentServerURL.getText();
    }

    public void setContentServerURL(String contentServerURL)
    {
        this.contentServerURL.setText(contentServerURL);
    }

    public boolean isRefsWindowAutoOpen()
    {
        return this.ckbRefsWindowAutoOpen.isSelected();
    }

    public void setRefsWindowAutoOpen(boolean refsWindowAutoOpen)
    {
        this.ckbRefsWindowAutoOpen.setSelected(refsWindowAutoOpen);
    }

    public boolean isRefsWindowAutoClose()
    {
        return this.ckbRefsWindowAutoClose.isSelected();
    }

    public void setRefsWindowAutoClose(boolean refsWindowAutoClose)
    {
        this.ckbRefsWindowAutoClose.setSelected(refsWindowAutoClose);
    }

    public Locale getScenamaticerLocale()
    {
        String lang = (String) this.scenamaticerLocale.getSelectedItem();
        assert lang != null;
        return switch (lang)
        {
            case "English" -> Locale.ENGLISH;
            case "日本語" -> Locale.JAPANESE;
            default -> DynamicBundle.getLocale();
        };
    }

    public void setScenamaticerLocale(Locale locale)
    {
        String lang = switch (locale.getLanguage())
        {
            case "en" -> "English";
            case "ja" -> "日本語";
            default -> "Default";
        };
        this.scenamaticerLocale.setSelectedItem(lang);
    }
}
