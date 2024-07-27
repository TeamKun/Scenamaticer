package org.kunlab.scenamatica.plugin.idea.editor;

import com.intellij.codeInsight.codeVision.CodeVisionAnchorKind;
import com.intellij.codeInsight.codeVision.CodeVisionEntry;
import com.intellij.codeInsight.codeVision.CodeVisionRelativeOrdering;
import com.intellij.codeInsight.codeVision.settings.CodeVisionGroupSettingProvider;
import com.intellij.codeInsight.codeVision.ui.model.CodeVisionPredefinedActionEntry;
import com.intellij.codeInsight.codeVision.ui.model.TextCodeVisionEntry;
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
import org.kunlab.scenamatica.plugin.idea.ScenamaticerBundle;
import org.kunlab.scenamatica.plugin.idea.ledger.LedgerManagerService;
import org.kunlab.scenamatica.plugin.idea.ledger.LedgerScenarioResolver;
import org.kunlab.scenamatica.plugin.idea.ledger.models.LedgerAction;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.lang.ScenarioFile;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.models.ScenarioType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ActionReferenceCodeVisionProvider implements DaemonBoundCodeVisionProvider, CodeVisionGroupSettingProvider
{

    @NotNull
    @Override
    public String getId()
    {
        return this.getClass().getName();
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
    public String getGroupId()
    {
        return "Scenamatica";
    }

    @Nls
    @NotNull
    @Override
    public String getDescription()
    {
        return ScenamaticerBundle.of("editor.codeVision.actionReference.description");
    }

    @Nls
    @NotNull
    @Override
    public String getGroupName()
    {
        return "Scenamatica: Action References";
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
                            return this.getActionReferencesCodeVision(file);
                    }),
                    new EmptyProgressIndicator()
            );
        }
        catch (ProcessCanceledException e)
        {
            return Collections.emptyList();
        }
    }

    private List<Pair<TextRange, CodeVisionEntry>> getActionReferencesCodeVision(@NotNull PsiFile file)
    {
        if (!(file instanceof ScenarioFile scenarioFile))
            return Collections.emptyList();

        LedgerScenarioResolver resolver = LedgerScenarioResolver.create(
                LedgerManagerService.getInstance(),
                scenarioFile
        ).detailedResolve();

        List<Pair<TextRange, CodeVisionEntry>> entries = new ArrayList<>();
        for (LedgerScenarioResolver.ResolveResult resolveResult : resolver.getActions())
        {
            LedgerAction action = resolveResult.getAction();
            if (action == null)
                continue;

            PsiElement element = resolveResult.getElement();
            ScenarioType usage = resolveResult.getUsage();

            entries.add(new Pair<>(
                    element.getTextRange(),
                    new ActionReferenceCodeVisionEntry(
                            element,
                            action.getName(),
                            action.getDescription(usage)
                    )
            ));
        }

        return entries;
    }

    @NotNull
    @Override
    public CodeVisionAnchorKind getDefaultAnchor()
    {
        return CodeVisionAnchorKind.Top;
    }

    private static class ActionReferenceCodeVisionEntry extends TextCodeVisionEntry implements CodeVisionPredefinedActionEntry
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
            Project proj = editor.getProject();
            if (proj == null)
                return;
            ReferenceNavigator.navigate(proj, editor, ReferenceNavigator.NavigateType.ACTION, this.actionName);
        }

        private static String getHint(@NotNull String actionName, @Nullable String actionDesc)
        {
            return ScenamaticerBundle.of(
                    "editor.codeVision.actionReference.hint",
                    actionName,
                    actionDesc == null ? ScenamaticerBundle.of("editor.codeVision.actionReference.hint.noDescription"): actionDesc
            );
        }
    }
}
