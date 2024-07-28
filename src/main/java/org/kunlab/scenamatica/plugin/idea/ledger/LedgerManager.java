package org.kunlab.scenamatica.plugin.idea.ledger;

import com.intellij.openapi.progress.ProgressManager;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kunlab.scenamatica.plugin.idea.ledger.models.ILedgerContent;
import org.kunlab.scenamatica.plugin.idea.ledger.models.LedgerAction;
import org.kunlab.scenamatica.plugin.idea.ledger.models.LedgerCategory;
import org.kunlab.scenamatica.plugin.idea.ledger.models.LedgerEvent;
import org.kunlab.scenamatica.plugin.idea.ledger.models.LedgerReference;
import org.kunlab.scenamatica.plugin.idea.ledger.models.LedgerType;

import java.util.Optional;
import java.util.function.BiFunction;

@Getter
public class LedgerManager
{
    public static final String ID_PRIME_TYPE = "ScenarioFileStructure";
    public static final LedgerReference REF_PRIME_TYPE = LedgerReference.of("$reference:type:" + ID_PRIME_TYPE);

    private final LedgersProvider provider;

    public LedgerManager()
    {
        this.provider = new LedgersProvider();

        // 初期化時にキャッシュを構築
        this.refresh();
    }

    public void refresh()
    {
        LedgerCacheBuildTask ledgerCacheBuildTask = new LedgerCacheBuildTask(true);
        ProgressManager.getInstance().run(ledgerCacheBuildTask);
    }

    @NotNull
    public LedgerType getPrimeType() // シナリオファイルの基底を取得
    {
        Ledger officialLedger = this.provider.getOfficialLedger();
        Optional<LedgerType> primeType = officialLedger.resolveReference(REF_PRIME_TYPE, LedgerType.class);
        if (primeType.isEmpty())
        {
            this.provider.buildCacheAll();
            primeType = officialLedger.resolveReference(REF_PRIME_TYPE, LedgerType.class);
            if (primeType.isEmpty())
                throw new IllegalStateException("Prime type not found.");
        }

        return primeType.get();
    }

    @NotNull
    public Optional<LedgerAction> getActionByID(@NotNull String id)
    {
        return this.getLedgerContentByID(id, Ledger::getActionByID);
    }

    @NotNull
    public Optional<LedgerType> getTypeByID(@NotNull String id)
    {
        return this.getLedgerContentByID(id, Ledger::getTypeByID);
    }

    @NotNull
    public Optional<LedgerEvent> getEventByID(@NotNull String id)
    {
        return this.getLedgerContentByID(id, Ledger::getEventByID);
    }

    @NotNull
    public Optional<LedgerCategory> getCategoryByID(@NotNull String id)
    {
        return this.getLedgerContentByID(id, Ledger::getCategoryByID);
    }

    @NotNull
    public <T extends ILedgerContent> Optional<T> resolveReference(@NotNull LedgerReference reference, @Nullable Class<? extends T> type)
    {
        for (Ledger ledger : this.provider.getLedgers())
        {
            Optional<T> content = ledger.resolveReference(reference, type);
            if (content.isPresent())
                return content;
        }

        return Optional.empty();
    }

    private <T extends ILedgerContent> Optional<T> getLedgerContentByID(@NotNull String id,
                                                                        @NotNull BiFunction<? super Ledger, ? super String, Optional<T>> getter)
    {
        for (Ledger ledger : this.provider.getLedgers())
        {
            Optional<T> content = getter.apply(ledger, id);
            if (content.isPresent())
                return content;
        }

        return Optional.empty();
    }
}
