package org.kunlab.scenamatica.plugin.idea.settings;

import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBTextField;
import javax.swing.JPanel;
import lombok.Getter;

public class ScenamaticerSettingsComponent
{
    @Getter
    private JPanel mainPanel;
    private JBTextField schemaURL;

    private JBCheckBox ckbRefsWindowEnabled;
    private JBCheckBox ckbRefsWindowAutoOpen;
    private JBCheckBox ckbRefsWindowAutoClose;

    public String getSchemaURL()
    {
        return this.schemaURL.getText();
    }

    public void setSchemaURL(String schemaURL)
    {
        this.schemaURL.setText(schemaURL);
    }

    public boolean isRefsWindowEnabled()
    {
        return this.ckbRefsWindowEnabled.isSelected();
    }

    public boolean isRefsWindowAutoOpen()
    {
        return this.ckbRefsWindowAutoOpen.isSelected();
    }

    public boolean isRefsWindowAutoClose()
    {
        return this.ckbRefsWindowAutoClose.isSelected();
    }
}
