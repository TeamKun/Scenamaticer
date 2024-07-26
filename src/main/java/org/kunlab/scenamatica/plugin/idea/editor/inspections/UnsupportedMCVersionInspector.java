package org.kunlab.scenamatica.plugin.idea.editor.inspections;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.ProblemsHolder;
import org.jetbrains.annotations.NotNull;
import org.kunlab.scenamatica.plugin.idea.ledger.LedgerScenarioResolver;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.lang.ScenarioFile;

public class UnsupportedMCVersionInspector extends AbstractScenarioFileInspection
{
    public static final String ID = "UnsupportedMCVersion";

    public UnsupportedMCVersionInspector()
    {
        super(ID, "Unsupported Minecraft Version", HighlightDisplayLevel.WEAK_WARNING);
    }

    @Override
    protected void visitScenarioFile(@NotNull ScenarioFile file, @NotNull ProblemsHolder holder, @NotNull LocalInspectionToolSession session)
    {
        this.reportDetailedResolveErrorTypeOf(file, holder, session, null, LedgerScenarioResolver.ResolveResult.InvalidCause.UNSUPPORTED_SERVER_VERSION);
    }
}
