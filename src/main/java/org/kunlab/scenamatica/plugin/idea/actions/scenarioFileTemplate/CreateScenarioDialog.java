package org.kunlab.scenamatica.plugin.idea.actions.scenarioFileTemplate;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.InputValidator;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.jdesktop.swingx.combobox.EnumComboBoxModel;
import org.jetbrains.annotations.Nullable;

public class CreateScenarioDialog extends DialogWrapper
{
    private final InputValidator validator;
    private JPanel centerPanel;
    private JTextField scenarioName;
    private JTextField fileName;
    private JTextField scenarioDescription;
    private JComboBox<String> scenamaticaVersion;
    private JCheckBox useDedicatedStageCheckBox;
    private JComboBox<StageEnvironment> stageEnvironment;
    private JComboBox<StageType> stageType;
    private JTextField stageSeed;
    private JPanel panelStage;
    private JCheckBox ckbTriggerManual;
    private JCheckBox ckbTriggerOnLoad;
    private JTextField originalWorld;

    protected CreateScenarioDialog(@Nullable Project project, InputValidator validator)
    {
        super(project, true, IdeModalityType.PROJECT);
        this.validator = validator;

        this.setTitle("Create new Scenamatica Scenario");
        this.init();

    }

    @Override
    protected void init()
    {
        super.init();
        this.setSize(700, 900);
        this.setOKActionEnabled(false);
        this.setOKButtonText("Create");

        this.panelStage.setVisible(false);
        this.panelStage.setEnabled(false);
        this.stageEnvironment.setModel(new EnumComboBoxModel<>(StageEnvironment.class));
        this.stageType.setModel(new EnumComboBoxModel<>(StageType.class));

        this.scenarioName.grabFocus();
        this.scenarioName.getDocument().addDocumentListener(new DocumentListener()
        {
            @Override
            public void insertUpdate(DocumentEvent e)
            {
                updateFileName();
                checkValid();
            }

            @Override
            public void removeUpdate(DocumentEvent e)
            {
                updateFileName();
                checkValid();
            }

            @Override
            public void changedUpdate(DocumentEvent e)
            {
                updateFileName();
                checkValid();
            }
        });

        this.useDedicatedStageCheckBox.addActionListener(e ->
        {
            boolean isSelected = ((JCheckBox) e.getSource()).isSelected();
            this.panelStage.setVisible(isSelected);
            this.panelStage.setEnabled(isSelected);

            this.originalWorld.setEnabled(!isSelected);
        });
    }

    private void checkValid()
    {
        boolean isNameValid = !this.scenarioName.getText().isEmpty();
        if (!isNameValid)
        {
            this.setOKActionEnabled(false);
            this.setErrorText("Scenario name cannot be empty.", this.scenarioName);
            return;
        }

        boolean isFileNameValid = !this.fileName.getText().isEmpty() && this.validator.checkInput(this.fileName.getText());
        if (!isFileNameValid)
        {
            this.setOKActionEnabled(false);
            this.setErrorText("Invalid file name.", this.fileName);
            return;
        }

        if (this.useDedicatedStageCheckBox.isSelected())
        {
            boolean isStageSeedValid = this.stageSeed.getText().isEmpty() || this.stageSeed.getText().matches("[0-9]+");
            if (!isStageSeedValid)
            {
                this.setOKActionEnabled(false);
                this.setErrorText("Invalid stage seed.", this.stageSeed);
                return;
            }
        }

        this.setOKActionEnabled(true);
        this.setErrorText(null);
    }

    private void updateFileName()
    {

        StringBuilder sb = new StringBuilder();
        String name = this.scenarioName.getText()
                .replaceAll("[^a-zA-Z0-9]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-", "")
                .replaceAll("-$", "");
        for (char c : name.toCharArray())
        {
            if ('A' <= c && c <= 'Z')
                sb.append("-").append(Character.toLowerCase(c));
            else
                sb.append(c);
        }

        String currentFileName = this.fileName.getText();
        String builtValue = sb.toString();
        boolean isEdited = !(currentFileName.isEmpty() || builtValue.startsWith(currentFileName));
        if (!isEdited)
            this.fileName.setText(builtValue);
    }

    public String getFileName()
    {
        String name = this.fileName.getText();
        if (name.endsWith(".yml") || name.endsWith(".yaml"))
            return name;
        else
            return name + ".yml";
    }

    public String getScenamaticaVersion()
    {
        return this.scenamaticaVersion.getSelectedItem().toString();
    }

    public String getScenarioName()
    {
        return this.scenarioName.getText();
    }

    public String getScenarioDescription()
    {
        return this.scenarioDescription.getText();
    }

    public boolean isUseDedicatedStage()
    {
        return this.useDedicatedStageCheckBox.isSelected();
    }

    public boolean canTriggerManually()
    {
        return this.ckbTriggerManual.isSelected();
    }

    public boolean shouldTriggerOnLoad()
    {
        return this.ckbTriggerOnLoad.isSelected();
    }

    public StageEnvironment getStageEnvironment()
    {
        return (StageEnvironment) this.stageEnvironment.getSelectedItem();
    }

    public StageType getStageType()
    {
        return (StageType) this.stageType.getSelectedItem();
    }

    public boolean hasOriginalWorld()
    {
        return !this.originalWorld.getText().isEmpty();
    }

    public String getOriginalWorld()
    {
        return this.originalWorld.getText();
    }

    public Long getStageSeed()
    {
        if (this.stageSeed.getText().isEmpty())
            return null;
        else
            return Long.parseLong(this.stageSeed.getText());
    }

    @Override
    protected @Nullable JComponent createCenterPanel()
    {
        return this.centerPanel;
    }
}
