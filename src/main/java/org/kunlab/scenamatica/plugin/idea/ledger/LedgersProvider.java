package org.kunlab.scenamatica.plugin.idea.ledger;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.PathManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kunlab.scenamatica.plugin.idea.settings.ScenamaticerSettingsState;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class LedgersProvider
{
    public static final Path LEDGER_PATH = PathManager.getSystemDir().resolve("scenamaticer-ledgers");

    private final Object lock = new Object();
    private final List<Ledger> ledgers;
    private List<Ledger> unmodifiableLedgers;
    private boolean isBusy;

    public LedgersProvider()
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
        this.unmodifiableLedgers = Collections.unmodifiableList(this.ledgers);
        return ledger.getLedgerName();
    }

    public void buildCacheAll()
    {
        if (this.isBusy)
        {
            try
            {
                synchronized (this.lock)
                {
                    this.lock.wait();
                }
            }
            catch (InterruptedException e)
            {
                throw new IllegalStateException(e);
            }
        }

        this.isBusy = true;
        for (Ledger ledger : this.ledgers)
            ledger.buildCache();

        this.isBusy = false;
        synchronized (this.lock)
        {
            this.lock.notifyAll();
        }
    }

    public void cleanCacheAll()
    {
        if (this.isBusy)
        {
            try
            {
                synchronized (this.lock)
                {
                    this.lock.wait();
                }
            }
            catch (InterruptedException e)
            {
                throw new IllegalStateException(e);
            }
        }

        this.isBusy = true;
        for (Ledger ledger : this.ledgers)
            ledger.cleanCache();

        this.isBusy = false;
        synchronized (this.lock)
        {
            this.lock.notifyAll();
        }
    }

    public void setOfficialLedgerURL(String setOfficialLedgerURL)
    {
        this.cleanCacheAll();
        Ledger officialLedger = this.getOfficialLedger();
        officialLedger.setLedgerURL(toURL(setOfficialLedgerURL));
    }

    @NotNull
    public Ledger getOfficialLedger()
    {
        Ledger officialLedger = this.getLedgerByName(Ledger.OFFICIAL_LEDGER_NAME);
        if (officialLedger == null)
            throw new IllegalStateException("Official ledger not found: " + Ledger.OFFICIAL_LEDGER_NAME + ", broken installation?");

        return officialLedger;
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

    @NotNull
    public List<Ledger> getLedgers()
    {
        return this.unmodifiableLedgers;
    }

    private static URL toURL(String urlString)
    {
        try
        {
            return new URI(urlString).toURL();
        }
        catch (URISyntaxException | MalformedURLException e)
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
