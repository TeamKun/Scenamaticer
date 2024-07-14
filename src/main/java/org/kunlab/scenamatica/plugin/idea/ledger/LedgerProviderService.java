package org.kunlab.scenamatica.plugin.idea.ledger;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import lombok.Getter;

@Getter
@Service(Service.Level.APP)
public final class LedgerProviderService
{
    private final LedgerProvider ledgerProvider;

    public LedgerProviderService()
    {
        this.ledgerProvider = new LedgerProvider();
    }

    public static LedgerProviderService getInstance()
    {
        return ApplicationManager.getApplication().getService(LedgerProviderService.class);
    }
}
