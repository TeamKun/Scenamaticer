package org.kunlab.scenamatica.plugin.idea.scenarioFile.lang;

import com.intellij.codeInsight.hint.HintManager;
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
import org.kunlab.scenamatica.plugin.idea.refsBrowser.RefsBrowserWindow;
import org.kunlab.scenamatica.plugin.idea.refsBrowser.WebReference;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.ScenarioFiles;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.schema.SchemaProviderService;
import org.kunlab.scenamatica.plugin.idea.utils.YAMLUtils;

public class ScenarioFileGotoDeclarationHandler implements GotoDeclarationHandler
{
    @Override
    public PsiElement @Nullable [] getGotoDeclarationTargets(@Nullable PsiElement psiElement, int i, Editor editor)
    {
        if (psiElement == null || !ScenarioFiles.isScenarioFile(psiElement.getContainingFile()))
            return null;

        if (YAMLUtils.isKey(psiElement))
            return new PsiElement[]{new FakeElement(psiElement, editor)};
        else
            return null;
    }

    private static class FakeElement extends FakePsiElement implements SyntheticElement
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

        public FakeElement(PsiElement element, Editor editor)
        {
            this.element = element;
            this.editor = editor;
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
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                try
                {
                    String typeName = SchemaProviderService.getResolver().getTypeName(this.element);
                    if (typeName == null)
                    {
                        ApplicationManager.getApplication().invokeLater(() -> HintManager.getInstance().showErrorHint(
                                this.editor,
                                "Cannot find the type of the reference"
                        ));
                    }
                    else
                    {
                        RefsBrowserWindow window = RefsBrowserWindow.getCurrentWindow(this.element.getProject());
                        if (window != null)
                            window.navigateTo(WebReference.typeToWebReference(typeName));
                    }
                }
                finally
                {
                    progressWindow.stop();
                }
            });
        }

        @Override
        public PsiElement getParent()
        {
            return this.element;
        }
    }
}
