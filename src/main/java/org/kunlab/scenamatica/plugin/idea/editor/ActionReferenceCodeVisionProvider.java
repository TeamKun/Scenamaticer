package org.kunlab.scenamatica.plugin.idea.editor;

import com.intellij.codeInsight.codeVision.CodeVisionAnchorKind;
import com.intellij.codeInsight.codeVision.CodeVisionEntry;
import com.intellij.codeInsight.codeVision.CodeVisionRelativeOrdering;
import com.intellij.codeInsight.codeVision.ui.model.CodeVisionPredefinedActionEntry;
import com.intellij.codeInsight.codeVision.ui.model.TextCodeVisionEntry;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.hints.codeVision.DaemonBoundCodeVisionProvider;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import kotlin.Pair;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLMapping;
import org.kunlab.scenamatica.plugin.idea.ScenamaticerBundle;
import org.kunlab.scenamatica.plugin.idea.refsBrowser.RefsBrowserWindow;
import org.kunlab.scenamatica.plugin.idea.refsBrowser.WebReference;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.ScenarioFiles;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.schema.SchemaProvider;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.schema.SchemaProviderService;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.schema.SchemaResolver;
import org.kunlab.scenamatica.plugin.idea.utils.YAMLUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class ActionReferenceCodeVisionProvider implements DaemonBoundCodeVisionProvider
{

    @NotNull
    @Override
    public String getId()
    {
        return "org.kunlab.scenamatica.plugin.idea.editor.ActionReferenceCodeVisionProvider";
    }

    @Nls
    @NotNull
    @Override
    public String getName()
    {
        return ScenamaticerBundle.of("editor.codeVision.actionReference.name");
    }

    @NotNull
    @Override
    public List<CodeVisionRelativeOrdering> getRelativeOrderings()
    {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public List<Pair<TextRange, CodeVisionEntry>> computeForEditor(@NotNull Editor editor, @NotNull PsiFile file)
    {
        Project proj = editor.getProject();
        if (proj == null || DumbService.isDumb(proj))
            return Collections.emptyList();

        try
        {
            return ProgressManager.getInstance().runProcess(
                    () -> ApplicationManager.getApplication().runReadAction((ThrowableComputable<List<Pair<TextRange, CodeVisionEntry>>, ProcessCanceledException>) () -> {
                        if (proj.isDisposed() || editor.isDisposed())
                            return Collections.emptyList();
                        else
                            return this.getActionReferencesCodeVision(proj, file);
                    }),
                    new EmptyProgressIndicator()
            );
        }
        catch (ProcessCanceledException e)
        {
            return Collections.emptyList();
        }
    }

    private List<Pair<TextRange, CodeVisionEntry>> getActionReferencesCodeVision(@NotNull Project project, @NotNull PsiFile file)
    {
        if (!acceptsFile(file))
            return Collections.emptyList();
        Iterator<PsiElement> elements = YAMLUtils.getDepthFirstIterator(file);

        List<Pair<TextRange, CodeVisionEntry>> entries = new ArrayList<>();
        while (elements.hasNext())
        {
            PsiElement element = elements.next();
            if (acceptsElement(element))
            {
                SchemaResolver.ScenarioAction scenarioAction = SchemaProviderService.getResolver().getAction(element);
                if (scenarioAction == null)
                    continue;

                String desc = null;
                SchemaProvider.Action action = SchemaProviderService.getProvider().getAction(scenarioAction.getName());
                if (action != null)
                    desc = action.getDescriptionFor(scenarioAction.getType());

                entries.add(new Pair<>(
                        element.getTextRange(),
                        new ActionReferenceCodeVisionEntry(
                                element,
                                scenarioAction.getName(),
                                desc
                        )
                ));
            }
        }

        return entries;
    }

    @NotNull
    @Override
    public CodeVisionAnchorKind getDefaultAnchor()
    {
        return CodeVisionAnchorKind.Top;
    }

    private static boolean acceptsElement(@NotNull PsiElement psiElement)
    {
        String type = SchemaProviderService.getResolver().getTypeName(psiElement);
        return psiElement instanceof YAMLMapping && ("scenario".equals(type) || "action".equals(type));
    }

    private static boolean acceptsFile(@NotNull PsiFile psiFile)
    {
        return ScenarioFiles.isScenarioFile(psiFile);
    }

    private class ActionReferenceCodeVisionEntry extends TextCodeVisionEntry implements CodeVisionPredefinedActionEntry
    {
        private final PsiElement element;
        private final String actionName;

        public ActionReferenceCodeVisionEntry(@NotNull PsiElement element, @NotNull String actionName, @Nullable String actionDesc)
        {
            super(
                    getHint(actionName, actionDesc),
                    ActionReferenceCodeVisionProvider.class.getName(),
                    null,
                    "('・ω・')",
                    ScenamaticerBundle.of("editor.codeVision.actionReference.tooltip"),
                    Collections.emptyList()
            );
            this.element = element;
            this.actionName = actionName;
        }

        @Override
        public void onClick(@NotNull Editor editor)
        {
            String reference = WebReference.actionToWebReference(this.actionName);
            if (reference == null)
            {
                showErrorMessage(editor, ScenamaticerBundle.of("editor.codeVision.actionReference.errors.unableToResolve"));
                return;
            }

            RefsBrowserWindow window = RefsBrowserWindow.getCurrentWindow(this.element.getProject());
            if (window == null)
            {
                showErrorMessage(editor, ScenamaticerBundle.of("editor.codeVision.actionReference.errors.unableToFindWindow"));
                return;
            }

            window.navigateTo(reference);
        }

        private static String getHint(@NotNull String actionName, @Nullable String actionDesc)
        {
            return ScenamaticerBundle.of(
                    "editor.codeVision.actionReference.hint",
                    actionName,
                    actionDesc == null ? ScenamaticerBundle.of("editor.codeVision.actionReference.hint.noDescription"): actionDesc
            );
        }

        private static void showErrorMessage(@NotNull Editor editor, @NotNull String message)
        {
            ApplicationManager.getApplication().invokeLater(() -> {
                HintManager.getInstance().showErrorHint(
                        editor,
                        message
                );
            });
        }
    }
}
