package org.kunlab.scenamatica.plugin.idea.ledger;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import org.jetbrains.annotations.NotNull;

public class LedgerCacheBuildTask extends Task.Backgroundable
{
    private final boolean dropBefore;

    public LedgerCacheBuildTask(boolean dropBefore)
    {
        super(null, "Building ledger cache", false);
        this.dropBefore = dropBefore;
    }

    @Override
    public void run(@NotNull ProgressIndicator progressIndicator)
    {
        progressIndicator.setIndeterminate(true);
        if (this.dropBefore)
            LedgerManagerService.getInstance().getProvider().cleanCacheAll();
        LedgerManagerService.getInstance().getProvider().buildCacheAll();
    }
}
