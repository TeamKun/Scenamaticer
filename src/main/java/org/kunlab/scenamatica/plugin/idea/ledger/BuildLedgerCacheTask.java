package org.kunlab.scenamatica.plugin.idea.ledger;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import org.jetbrains.annotations.NotNull;

public class BuildLedgerCacheTask extends Task.Backgroundable
{
    public BuildLedgerCacheTask()
    {
        super(null, "Building ledger cache", false);
    }

    @Override
    public void run(@NotNull ProgressIndicator progressIndicator)
    {
        LedgerProviderService.getInstance().getLedgerProvider().buildCache();
    }
}
