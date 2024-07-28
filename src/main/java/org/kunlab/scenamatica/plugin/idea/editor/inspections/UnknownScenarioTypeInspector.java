package org.kunlab.scenamatica.plugin.idea.editor.inspections;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.ProblemsHolder;
import org.jetbrains.annotations.NotNull;
import org.kunlab.scenamatica.plugin.idea.ledger.LedgerScenarioResolver;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.lang.ScenarioFile;

public class UnknownScenarioTypeInspector extends AbstractScenarioFileInspection
{
    public static final String ID = "UnsupportedScenarioType";

    public UnknownScenarioTypeInspector()
    {
        super(ID, "Unsupported Scenario Type", HighlightDisplayLevel.ERROR);
    }

    @Override
    protected void visitScenarioFile(@NotNull ScenarioFile file, @NotNull ProblemsHolder holder, @NotNull LocalInspectionToolSession session)
    {
        this.reportDetailedResolveErrorTypeOf(
                file,
                holder,
                session,
                null,
                this::traverseKVValue,
                LedgerScenarioResolver.ResolveResult.InvalidCause.UNKNOWN_SCENARIO_TYPE
        );
    }
}
