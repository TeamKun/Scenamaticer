package org.kunlab.scenamatica.plugin.idea.editor.inspections.action;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.ProblemsHolder;
import org.jetbrains.annotations.NotNull;
import org.kunlab.scenamatica.plugin.idea.editor.inspections.AbstractScenarioFileInspection;
import org.kunlab.scenamatica.plugin.idea.ledger.LedgerScenarioResolver;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.lang.ScenarioFile;

public class UnsupportedUsageInspector extends AbstractScenarioFileInspection
{
    public static final String ID = "UnsupportedActionUsage";

    public UnsupportedUsageInspector()
    {
        super(ID, "UnsupportedActionUsage", HighlightDisplayLevel.ERROR);
    }

    @Override
    protected void visitScenarioFile(@NotNull ScenarioFile file, @NotNull ProblemsHolder holder, @NotNull LocalInspectionToolSession session)
    {
        this.reportDetailedResolveErrorTypeOf(
                file,
                holder,
                session,
                null,
                LedgerScenarioResolver.ResolveResult.InvalidCause.ACTION_USAGE_VIOLATION
        );
    }
}
