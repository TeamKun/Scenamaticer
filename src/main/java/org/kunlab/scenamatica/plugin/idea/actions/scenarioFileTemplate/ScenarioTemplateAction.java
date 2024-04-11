package org.kunlab.scenamatica.plugin.idea.actions.scenarioFileTemplate;

import com.intellij.ide.actions.CreateElementActionBase;
import com.intellij.ide.actions.CreateFileAction;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.fileTemplates.FileTemplateUtil;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import org.jetbrains.annotations.NotNull;
import org.kunlab.scenamatica.plugin.idea.ScenamaticerBundle;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.ScenamaticaIcons;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.models.StageEnvironment;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.models.StageType;

import java.util.Properties;
import java.util.function.Consumer;

public class ScenarioTemplateAction extends CreateFileAction
{
    public static final String TEMPLATE_NAME = "Scenamatica Scenario File";

    private CreateScenarioDialog dialog;

    public ScenarioTemplateAction()
    {
        super(
                ScenamaticerBundle.lazy("actions.scenarioFileTemplate.name"),
                ScenamaticerBundle.lazy("actions.scenarioFileTemplate.description"),
                () -> ScenamaticaIcons.ICON
        );
    }

    @Override
    protected void invokeDialog(@NotNull Project project, @NotNull PsiDirectory directory, @NotNull Consumer<? super PsiElement[]> elementsConsumer)
    {
        CreateElementActionBase.MyInputValidator validator = new MyValidator(project, directory);
        this.dialog = new CreateScenarioDialog(project, validator, directory);
        this.dialog.show();

        if (this.dialog.isOK())
        {
            validator.tryCreate(this.dialog.getFileName());
            elementsConsumer.accept(validator.getCreatedElements());
        }
    }

    @Override
    protected PsiElement @NotNull [] create(@NotNull String newName, @NotNull PsiDirectory directory) throws Exception
    {
        if (this.dialog == null)
            return PsiElement.EMPTY_ARRAY;

        Project project = directory.getProject();
        FileTemplate template = FileTemplateManager.getInstance(project).getInternalTemplate(TEMPLATE_NAME);

        Properties props = new Properties(FileTemplateManager.getInstance(project).getDefaultProperties());
        this.createTemplateProperties(props);


        PsiFile psiFile = FileTemplateUtil.createFromTemplate(template, newName, props, directory).getContainingFile();
        SmartPsiElementPointer<PsiFile> pointer = SmartPointerManager.getInstance(project).createSmartPsiElementPointer(psiFile);
        VirtualFile virtualFile = psiFile.getVirtualFile();
        if (virtualFile != null)
            FileEditorManager.getInstance(project).openFile(virtualFile, true);

        return new PsiElement[]{pointer.getElement()};
    }

    private void createTemplateProperties(Properties props)
    {
        props.setProperty("SCENAMATICA_VERSION", wrapYAMLString(this.dialog.getScenamaticaVersion()));
        props.setProperty("SCENARIO_NAME", wrapYAMLString(this.dialog.getScenarioName()));
        if (!this.dialog.getScenarioDescription().isEmpty())
            props.setProperty("SCENARIO_DESCRIPTION", wrapYAMLString(this.dialog.getScenarioDescription()));

        // Trigger
        boolean hasTrigger = false;
        if (hasTrigger |= this.dialog.canTriggerManually())
            props.setProperty("TRIGGER_MANUALLY_DISPATCH", "true");
        if (hasTrigger |= this.dialog.shouldTriggerOnLoad())
            props.setProperty("TRIGGER_ON_LOAD", "true");
        if (!hasTrigger)
            props.setProperty("NO_TRIGGER", "true");

        boolean contextUsage = false;
        // Stage
        if (contextUsage |= this.dialog.isUseDedicatedStage())
        {
            props.setProperty("STAGE_USAGE", "true");
            if (this.dialog.getStageType() != StageType.NORMAL)
                props.setProperty("STAGE_TYPE", wrapYAMLString(this.dialog.getStageType().name()));
            if (this.dialog.getStageEnvironment() != StageEnvironment.OVER_WORLD)
                props.setProperty("STAGE_ENVIRONMENT", wrapYAMLString(this.dialog.getStageEnvironment().name()));
            if (this.dialog.getStageSeed() != null)
                props.setProperty("STAGE_SEED", wrapYAMLString(this.dialog.getStageSeed().toString()));
        }
        else //noinspection ConstantValue
            if (contextUsage |= this.dialog.hasOriginalWorld())
            {
                props.setProperty("STAGE_USAGE", "true");
                props.setProperty("STAGE_ORIGINAL_NAME", wrapYAMLString(this.dialog.getOriginalWorld()));
            }

        if (contextUsage)
            props.setProperty("CONTEXT_USAGE", "true");

        props.setProperty("AUTO_GENERATED_STUB", ScenamaticerBundle.of("templates.scenarioFileTemplate.comments.autoGeneratedStub"));
    }

    @Override
    public boolean isDumbAware()
    {
        return true;
    }

    @Override
    protected @NlsContexts.DialogTitle String getErrorTitle()
    {
        return "Unable to create Scenario file";
    }

    @Override
    protected @NlsContexts.Command @NotNull String getActionName(@NotNull PsiDirectory psiDirectory, @NotNull String s)
    {
        return "scenmaticer.actions.create.scenario";
    }

    private static String wrapYAMLString(String s)
    {
        return s == null ? "": s.replace("\n", "\n  ");
    }
}
