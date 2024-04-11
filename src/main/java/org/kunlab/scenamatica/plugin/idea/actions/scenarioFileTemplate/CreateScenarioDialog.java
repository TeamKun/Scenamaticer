package org.kunlab.scenamatica.plugin.idea.actions.scenarioFileTemplate;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.InputValidator;
import com.intellij.psi.PsiDirectory;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.jdesktop.swingx.combobox.EnumComboBoxModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kunlab.scenamatica.plugin.idea.ScenamaticerBundle;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.index.ScenarioFileIndexer;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.models.StageEnvironment;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.models.StageType;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.policy.MinecraftVersion;

public class CreateScenarioDialog extends DialogWrapper
{
    private final Project project;
    private final InputValidator validator;
    private final PsiDirectory directory;
    private JPanel centerPanel;
    private JBTextField tbScenarioName;
    private JBTextField tbFileName;
    private JBTextField tbScenarioDescription;
    private JComboBox<String> cbScenamaticaVersion;
    private JCheckBox ckbUseDedicatedStage;
    private JComboBox<StageEnvironment> cbStageEnvironment;
    private JComboBox<StageType> cbStageType;
    private JTextField tbStageSeed;
    private JPanel pnlCreateStage;
    private JCheckBox ckbTriggerManual;
    private JCheckBox ckbTriggerOnLoad;
    private JTextField tbOriginalWorld;
    private JLabel lbName;
    private JLabel lbFileName;
    private JLabel lbDescription;
    private JLabel lbStage;
    private JLabel lbOr;
    private JLabel lbCopyAnExistingStageFrom;
    private JLabel lbStageType;
    private JLabel lbStageSeed;
    private JLabel lbExecution;
    private JLabel lbTriggers;
    private JLabel lbScenamaticaVersion;
    private JLabel lbStageEnvironment;
    private JComboBox<String> cbMCSince;
    private JComboBox<String> cbMCUntil;
    private JBLabel lbMCSince;
    private JBLabel lbMCVersion;
    private JLabel lbMCUntil;

    protected CreateScenarioDialog(@Nullable Project project, InputValidator validator, PsiDirectory directory)
    {
        super(project, true, IdeModalityType.PROJECT);
        this.project = project;
        this.validator = validator;
        this.directory = directory;

        this.setTitle(ScenamaticerBundle.of("actions.scenarioFileTemplate.ui.title"));
        this.init();

    }

    private void localizeComponents()
    {
        ScenamaticerBundle.embed(this.lbName, "actions.scenarioFileTemplate.ui.scenarioName");
        ScenamaticerBundle.embed(this.lbFileName, "actions.scenarioFileTemplate.ui.fileName");
        ScenamaticerBundle.embed(this.lbDescription, "actions.scenarioFileTemplate.ui.description");
        ScenamaticerBundle.embed(this.lbStage, "actions.scenarioFileTemplate.ui.stage");
        ScenamaticerBundle.embed(this.ckbUseDedicatedStage, "actions.scenarioFileTemplate.ui.stage.options.createDedicated");
        ScenamaticerBundle.embed(this.lbOr, "actions.scenarioFileTemplate.ui.stage.options.or");
        ScenamaticerBundle.embed(this.lbCopyAnExistingStageFrom, "actions.scenarioFileTemplate.ui.stage.options.copyFrom");
        ScenamaticerBundle.embed(this.lbStageType, "actions.scenarioFileTemplate.ui.stage.options.createDedicated.type");
        ScenamaticerBundle.embed(this.lbStageEnvironment, "actions.scenarioFileTemplate.ui.stage.options.createDedicated.environment");
        ScenamaticerBundle.embed(this.lbStageSeed, "actions.scenarioFileTemplate.ui.stage.options.createDedicated.seed");
        ScenamaticerBundle.embed(this.lbExecution, "actions.scenarioFileTemplate.ui.execution");
        ScenamaticerBundle.embed(this.lbTriggers, "actions.scenarioFileTemplate.ui.execution.triggers");
        ScenamaticerBundle.embed(this.ckbTriggerManual, "actions.scenarioFileTemplate.ui.execution.onManuallyDispatch");
        ScenamaticerBundle.embed(this.ckbTriggerOnLoad, "actions.scenarioFileTemplate.ui.execution.onLoad");
        ScenamaticerBundle.embed(this.lbScenamaticaVersion, "actions.scenarioFileTemplate.ui.scenamaticaVersion");
        ScenamaticerBundle.embed(this.lbMCVersion, "actions.scenarioFileTemplate.ui.minecraftVersion");
        ScenamaticerBundle.embed(this.lbMCSince, "actions.scenarioFileTemplate.ui.minecraftVersion.since");
        ScenamaticerBundle.embed(this.lbMCUntil, "actions.scenarioFileTemplate.ui.minecraftVersion.until");
    }

    @Override
    protected void init()
    {
        super.init();
        this.setSize(700, 900);
        this.setOKActionEnabled(false);
        this.setOKButtonText(ScenamaticerBundle.of("actions.scenarioFileTemplate.ui.create"));
        this.localizeComponents();

        this.pnlCreateStage.setVisible(false);
        this.pnlCreateStage.setEnabled(false);
        this.cbStageEnvironment.setModel(new EnumComboBoxModel<>(StageEnvironment.class));
        this.cbStageType.setModel(new EnumComboBoxModel<>(StageType.class));

        this.initMinecraftVersions();

        this.tbScenarioName.grabFocus();
        this.tbScenarioName.getDocument().addDocumentListener(new DocumentListener()
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

        this.tbFileName.getDocument().addDocumentListener(new DocumentListener()
        {
            @Override
            public void insertUpdate(DocumentEvent e)
            {
                checkValid();
            }

            @Override
            public void removeUpdate(DocumentEvent e)
            {
                checkValid();
            }

            @Override
            public void changedUpdate(DocumentEvent e)
            {
                checkValid();
            }
        });

        this.ckbUseDedicatedStage.addActionListener(e ->
        {
            boolean isSelected = ((JCheckBox) e.getSource()).isSelected();
            this.pnlCreateStage.setVisible(isSelected);
            this.pnlCreateStage.setEnabled(isSelected);

            this.tbOriginalWorld.setEnabled(!isSelected);
        });
    }

    private void initMinecraftVersions()
    {
        MinecraftVersion[] versions = MinecraftVersion.values();
        for (MinecraftVersion version : versions)
        {
            if (!version.toString().contains("_X"))
            {
                this.cbMCSince.addItem(version.getVersion());
                this.cbMCUntil.addItem(version.getVersion());
            }
        }

        this.cbMCSince.setSelectedItem(MinecraftVersion.ANY.getVersion());
        this.cbMCUntil.setSelectedItem(MinecraftVersion.ANY.getVersion());
    }

    private MinecraftVersion getSelectedMinecraftVersion(JComboBox<String> comboBox)
    {
        Object selectedItem = comboBox.getSelectedItem();
        if (selectedItem == null)
            return MinecraftVersion.ANY;

        return MinecraftVersion.fromString(selectedItem.toString());
    }

    private void checkValid()
    {
        boolean errors = false;
        JComponent errorComponent;

        errorComponent = this.tbScenarioName;
        {
            String errorMessage = null;
            if (this.tbScenarioName.getText().isEmpty())
                errorMessage = ScenamaticerBundle.of("actions.scenarioFileTemplate.ui.scenarioName.errors.empty");
            else if (ScenarioFileIndexer.hasIndexFor(this.project, this.tbScenarioName.getText()))
                errorMessage = ScenamaticerBundle.of("actions.scenarioFileTemplate.ui.scenarioName.errors.duplicated", this.tbScenarioName.getText());

            if (errorMessage != null)
            {
                errors = true;
                this.setOKActionEnabled(false);
                this.setErrorText(errorMessage, errorComponent);
            }
        }

        errorComponent = this.tbFileName;
        {
            String fileName = this.tbFileName.getText();
            String errorMessage = null;
            if (fileName.isEmpty())
                errorMessage = ScenamaticerBundle.of("actions.scenarioFileTemplate.ui.fileName.errors.empty");
            else if (!this.validator.checkInput(fileName))
                errorMessage = ScenamaticerBundle.of("actions.scenarioFileTemplate.ui.fileName.errors.invalid", fileName);
            else if (this.directory.findFile(fileName + ".yml") != null)
                errorMessage = ScenamaticerBundle.of("actions.scenarioFileTemplate.ui.fileName.errors.duplicated", fileName);
            if (errorMessage != null)
            {
                errors = true;
                this.setOKActionEnabled(false);
                this.setErrorText(errorMessage, errorComponent);
            }

        }

        if (this.ckbUseDedicatedStage.isSelected())
        {
            errorComponent = this.tbStageSeed;
            {
                String errorMessage = null;
                boolean isStageSeedValid = this.tbStageSeed.getText().isEmpty() || this.tbStageSeed.getText().matches("^-?[0-9]+$");
                if (!isStageSeedValid)
                    errorMessage = ScenamaticerBundle.of("actions.scenarioFileTemplate.ui.stage.options.createDedicated.seed.errors.invalid");

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
        String name = this.tbScenarioName.getText()
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

        String currentFileName = this.tbFileName.getText();
        String builtValue = sb.toString();
        boolean isEdited = !(currentFileName.isEmpty() || builtValue.startsWith(currentFileName));
        if (!isEdited)
            this.tbFileName.setText(builtValue);
    }

    public String getFileName()
    {
        String name = this.tbFileName.getText();
        if (name.endsWith(".yml") || name.endsWith(".yaml"))
            return name;
        else
            return name + ".yml";
    }

    public String getScenamaticaVersion()
    {
        return this.cbScenamaticaVersion.getSelectedItem().toString();
    }

    public String getScenarioName()
    {
        return this.tbScenarioName.getText();
    }

    public String getScenarioDescription()
    {
        return this.tbScenarioDescription.getText();
    }

    public boolean isUseDedicatedStage()
    {
        return this.ckbUseDedicatedStage.isSelected();
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
        return (StageEnvironment) this.cbStageEnvironment.getSelectedItem();
    }

    public StageType getStageType()
    {
        return (StageType) this.cbStageType.getSelectedItem();
    }

    public boolean hasOriginalWorld()
    {
        return !this.tbOriginalWorld.getText().isEmpty();
    }

    public String getOriginalWorld()
    {
        return this.tbOriginalWorld.getText();
    }

    public Long getStageSeed()
    {
        if (this.tbStageSeed.getText().isEmpty())
            return null;
        else
            return Long.parseLong(this.tbStageSeed.getText());
    }

    @NotNull
    public MinecraftVersion getMinecraftSince()
    {
        return this.getSelectedMinecraftVersion(this.cbMCSince);
    }

    @NotNull
    public MinecraftVersion getMinecraftUntil()
    {
        return this.getSelectedMinecraftVersion(this.cbMCUntil);
    }

    @Override
    protected @Nullable JComponent createCenterPanel()
    {
        return this.centerPanel;
    }
}
