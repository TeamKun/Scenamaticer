package org.kunlab.scenamatica.plugin.idea.ledger;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import lombok.Getter;

@Getter
@Service(Service.Level.APP)
public final class LedgerManagerService
{
    private final LedgerManager manager;

    public LedgerManagerService()
    {
        this.manager = new LedgerManager();
    }

    public static LedgerManager getInstance()
    {
        return ApplicationManager.getApplication().getService(LedgerManagerService.class).manager;
    }
}
