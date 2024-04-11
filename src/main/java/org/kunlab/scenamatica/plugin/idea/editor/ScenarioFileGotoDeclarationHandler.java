package org.kunlab.scenamatica.plugin.idea.editor;

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
import org.kunlab.scenamatica.plugin.idea.scenarioFile.lang.ScenarioFileType;
import org.kunlab.scenamatica.plugin.idea.utils.YAMLUtils;

public class ScenarioFileGotoDeclarationHandler implements GotoDeclarationHandler
{
    @Override
    public PsiElement @Nullable [] getGotoDeclarationTargets(@Nullable PsiElement psiElement, int i, Editor editor)
    {
        if (psiElement == null || ScenarioFileType.isType(psiElement.getContainingFile()))
            return null;


        if (YAMLUtils.isKey(psiElement) || WebReference.isActionSpecifier(psiElement))
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
            try
            {
                ApplicationManager.getApplication().executeOnPooledThread(() -> actualNavigate(this.element, this.editor));
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

        private static void actualNavigate(PsiElement element, Editor editor)
        {
            String reference = WebReference.findElementReferenceURL(element);
            RefsBrowserWindow window = RefsBrowserWindow.getCurrentWindow(element.getProject());
            if (reference == null || window == null)
            {
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (reference == null)
                    {
                        HintManager.getInstance().showErrorHint(
                                editor,
                                "Cannot find the reference of this element."
                        );
                    }
                    else /* assert window == null */
                    {
                        HintManager.getInstance().showErrorHint(
                                editor,
                                "Unable to find the reference window."
                        );
                    }
                });
                return;
            }

            window.navigateTo(reference);
        }
    }
}
