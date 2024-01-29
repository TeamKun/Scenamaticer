package org.kunlab.scenamatica.plugin.idea.actions.scenarioFileTemplate;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.InputValidator;
import com.intellij.psi.PsiDirectory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.jdesktop.swingx.combobox.EnumComboBoxModel;
import org.jetbrains.annotations.Nullable;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.index.ScenarioFileIndexer;

public class CreateScenarioDialog extends DialogWrapper
{
    private final Project project;
    private final InputValidator validator;
    private final PsiDirectory directory;
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

    protected CreateScenarioDialog(@Nullable Project project, InputValidator validator, PsiDirectory directory)
    {
        super(project, true, IdeModalityType.PROJECT);
        this.project = project;
        this.validator = validator;
        this.directory = directory;

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
        boolean errors = false;
        JComponent errorComponent = null;

        errorComponent = this.scenarioName;
        {
            String errorMessage = null;
            if (this.scenarioName.getText().isEmpty())
                errorMessage = "Scenario name cannot be empty.";
            else if (ScenarioFileIndexer.hasIndexFor(this.project, this.scenarioName.getText()))
                errorMessage = "Scenario name is duplicated.";

            if (errorMessage != null)
            {
                errors = true;
                this.setOKActionEnabled(false);
                this.setErrorText(errorMessage, errorComponent);
            }
        }

        errorComponent = this.fileName;
        {
            String errorMessage = null;
            if (this.fileName.getText().isEmpty())
                errorMessage = "File name cannot be empty.";
            else if (!this.validator.checkInput(this.fileName.getText()))
                errorMessage = "File name is invalid.";
            else if (this.directory.findFile(this.fileName.getText() + ".yml") != null)
                errorMessage = "File name is duplicated.";

            if (errorMessage != null)
            {
                errors = true;
                this.setOKActionEnabled(false);
                this.setErrorText(errorMessage, errorComponent);
            }

        }

        if (this.useDedicatedStageCheckBox.isSelected())
        {
            errorComponent = this.stageSeed;
            {
                String errorMessage = null;
                boolean isStageSeedValid = this.stageSeed.getText().isEmpty() || this.stageSeed.getText().matches("[0-9]+");
                if (!isStageSeedValid)
                    errorMessage = "Stage seed is invalid.";

                if (errorMessage != null)
                {
                    errors = true;
                    this.setOKActionEnabled(false);
                    this.setErrorText(errorMessage, errorComponent);
                }
            }

        }

        if (errors)
        {
            this.setOKActionEnabled(true);
            this.setErrorText(null);
        }
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
