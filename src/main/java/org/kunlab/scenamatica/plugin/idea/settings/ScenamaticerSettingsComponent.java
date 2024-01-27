package org.kunlab.scenamatica.plugin.idea.settings;

import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import javax.swing.JPanel;
import lombok.Getter;

public class ScenamaticerSettingsComponent
{
    @Getter
    private final JPanel mainPanel;
    private final JBTextField schemaURLField;

    public ScenamaticerSettingsComponent()
    {
        this.mainPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("Schema URL: "), this.schemaURLField = new JBTextField())
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
    }

    public String getSchemaURL()
    {
        return this.schemaURLField.getText();
    }

    public void setSchemaURL(String schemaURL)
    {
        this.schemaURLField.setText(schemaURL);
    }
}
