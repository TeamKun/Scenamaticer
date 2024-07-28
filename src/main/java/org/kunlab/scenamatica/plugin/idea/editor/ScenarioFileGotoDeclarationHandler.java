package org.kunlab.scenamatica.plugin.idea.editor;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.util.ProgressWindow;
import com.intellij.psi.PsiElement;
import com.intellij.psi.SyntheticElement;
import com.intellij.psi.impl.FakePsiElement;
import javax.swing.Icon;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.kunlab.scenamatica.plugin.idea.ledger.LedgerManagerService;
import org.kunlab.scenamatica.plugin.idea.ledger.LedgerScenarioResolver;
import org.kunlab.scenamatica.plugin.idea.ledger.models.LedgerAction;
import org.kunlab.scenamatica.plugin.idea.ledger.models.LedgerType;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.lang.ScenarioFile;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.lang.ScenarioFileType;
import org.kunlab.scenamatica.plugin.idea.utils.YAMLUtils;

public class ScenarioFileGotoDeclarationHandler implements GotoDeclarationHandler
{
    @Override
    public PsiElement @Nullable [] getGotoDeclarationTargets(@Nullable PsiElement psiElement, int i, Editor editor)
    {
        if (psiElement == null || !ScenarioFileType.isType(psiElement.getContainingFile()) || !YAMLUtils.isKey(psiElement))
            return null;
        ScenarioFile file = (ScenarioFile) psiElement.getContainingFile();
        YAMLKeyValue kv = (YAMLKeyValue) psiElement.getParent();

        LedgerScenarioResolver resolver = LedgerScenarioResolver.create(
                LedgerManagerService.getInstance(),
                file
        ).detailedResolve();

        LedgerScenarioResolver.ResolveResult result = resolver.getResultForElement(kv);
        if (result == null)
            return null;

        if (result.getType() != null)
            return new PsiElement[]{new NavigatonFakeElement(psiElement, editor, result.getType())};
        else if (result.getAction() != null)
            return new PsiElement[]{new NavigatonFakeElement(psiElement, editor, result.getAction())};
        else
            return null;
    }

    private static class NavigatonFakeElement extends FakePsiElement implements SyntheticElement
    {
        private static final ItemPresentation PRESENTATION = new ItemPresentation()
        {
            @Override
            public String getPresentableText()
            {
                return "Navigate to the reference";
            }

            @Override
            public String getLocationString()
            {
                return "Web";
            }

            @Override
            public @Nullable Icon getIcon(boolean b)
            {
                return null;
            }
        };

        private final PsiElement element;
        private final Editor editor;
        private final LedgerAction action;
        private final LedgerType type;

        public NavigatonFakeElement(PsiElement element, Editor editor, LedgerAction action)
        {
            this.element = element;
            this.editor = editor;
            this.action = action;
            this.type = null;
        }

        public NavigatonFakeElement(PsiElement element, Editor editor, LedgerType type)
        {
            this.element = element;
            this.editor = editor;
            this.action = null;
            this.type = type;
        }

        @Override
        public String getPresentableText()
        {
            return "Navigate to the reference";
        }

        @Override
        public @Nullable @NonNls String getText()
        {
            return "Navigate to the reference";
        }

        @Override
        public String getName()
        {
            return "Navigate to the reference";
        }

        @Override
        public ItemPresentation getPresentation()
        {
            return PRESENTATION;
        }

        @Override
        public void navigate(boolean requestFocus)
        {
            ProgressWindow progressWindow = new ProgressWindow(false, this.element.getProject());
            progressWindow.setTitle("Navigating to the reference");
            progressWindow.setIndeterminate(true);
            progressWindow.start();
            try
            {
                ApplicationManager.getApplication().executeOnPooledThread(() -> this.actualNavigate(this.element, this.editor));
            }
            finally
            {
                progressWindow.stop();
            }
        }

        @Override
        public PsiElement getParent()
        {
            return this.element;
        }

        private void actualNavigate(PsiElement element, Editor editor)
        {
            if (this.action != null)
                ReferenceNavigator.navigateToActionReference(element.getProject(), editor, this.action);
            else if (this.type != null)
                ReferenceNavigator.navigateToTypeReference(element.getProject(), editor, this.type);
        }
    }
}
