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
import org.jetbrains.yaml.psi.YAMLMapping;
import org.kunlab.scenamatica.plugin.idea.ScenamaticerBundle;
import org.kunlab.scenamatica.plugin.idea.ledger.ScenarioLedgerLinker;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.lang.ScenarioFileType;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.schema.SchemaAction;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.schema.SchemaProviderService;
import org.kunlab.scenamatica.plugin.idea.utils.YAMLUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
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
        if (!acceptsFile(file))
            return Collections.emptyList();
        Iterator<PsiElement> elements = YAMLUtils.getDepthFirstIterator(file);

        List<Pair<TextRange, CodeVisionEntry>> entries = new ArrayList<>();
        while (elements.hasNext())
        {
            PsiElement element = elements.next();
            if (acceptsElement(element))
            {
                ScenarioLedgerLinker.ScenarioAction scenarioAction = SchemaProviderService.getResolver().getAction(element);
                if (scenarioAction == null)
                    continue;

                SchemaAction action = SchemaProviderService.getProvider().getAction(scenarioAction.getType());
                if (action == null)
                    continue;

                entries.add(new Pair<>(
                        element.getTextRange(),
                        new ActionReferenceCodeVisionEntry(
                                element,
                                scenarioAction.getType(),
                                action.getDescriptionFor(scenarioAction.getType())
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
        return ScenarioFileType.isType(psiFile);
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
            ActionReferences.navigate(editor.getProject(), editor, this.actionName);
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
