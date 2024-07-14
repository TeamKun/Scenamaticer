package org.kunlab.scenamatica.plugin.idea.ledger;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.PathManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kunlab.scenamatica.plugin.idea.settings.ScenamaticerSettingsState;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class LedgerProvider
{
    private static final Path LEDGER_PATH = PathManager.getSystemDir().resolve("scenamaticer-ledgers");

    private final List<Ledger> ledgers;

    public LedgerProvider()
    {
        this.ledgers = new ArrayList<>();

        this.registerLedger(
                Ledger.OFFICIAL_LEDGER_NAME,
                toURL(ScenamaticerSettingsState.getInstance().getOfficialLedgerURL())
        );
    }

    public String registerLedger(@NotNull String ledgerName, @NotNull URL ledgerURL)
    {
        Ledger ledger = new Ledger(ledgerName, LEDGER_PATH, ledgerURL);
        this.ledgers.add(ledger);
        return ledger.getLedgerName();
    }

    public void buildCache()
    {
        for (Ledger ledger : this.ledgers)
            ledger.buildCache();
    }

    public void clearCache()
    {
        for (Ledger ledger : this.ledgers)
            ledger.cleanCache();
    }

    public void setOfficialLedgerURL(String setOfficialLedgerURL)
    {
        this.clearCache();
        Ledger officialLedger = this.getLedgerByName(Ledger.OFFICIAL_LEDGER_NAME);
        assert officialLedger != null;
        officialLedger.setLedgerURL(toURL(setOfficialLedgerURL));
    }

    @Nullable
    public Ledger getLedgerByName(String ledgerName)
    {
        for (Ledger ledger : this.ledgers)
        {
            if (ledger.getLedgerName().equals(ledgerName))
                return ledger;
        }

        return null;
    }

    private static URL toURL(String urlString)
    {
        try
        {
            return new URL(urlString);
        }
        catch (MalformedURLException e)
        {
            NotificationGroupManager.getInstance()
                    .getNotificationGroup("Scenamatica")
                    .createNotification(
                            "Invalid URL: " + urlString,
                            NotificationType.ERROR
                    )
                    .notify(null);

            throw new IllegalStateException(e);
        }
    }
}
