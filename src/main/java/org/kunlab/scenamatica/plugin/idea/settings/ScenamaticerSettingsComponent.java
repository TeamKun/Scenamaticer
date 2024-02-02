package org.kunlab.scenamatica.plugin.idea.settings;

import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBTextField;
import javax.swing.JPanel;
import javax.swing.JTextField;
import lombok.Getter;

public class ScenamaticerSettingsComponent
{
    @Getter
    private JPanel mainPanel;
    private JBTextField schemaURL;
    private JTextField contentServerURL;

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

    public void setRefsWindowEnabled(boolean refsWindowEnabled)
    {
        this.ckbRefsWindowEnabled.setSelected(refsWindowEnabled);
    }
}
