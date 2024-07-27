package org.kunlab.scenamatica.plugin.idea.ledger.models;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * アドモニションの種類を表す列挙型です。
 */
@AllArgsConstructor
@Getter
public enum AdmonitionType
{
    /**
     * 注釈や追加事項, その他の情報を表します。
     */
    NOTE("note"),
    /**
     * ヒントやアドバイスを表します。
     */
    TIP("tip"),
    /**
     * 追加情報を表します。
     */
    INFORMATION("info"),
    /**
     * 警告を表します。
     */
    WARNING("warning"),
    /**
     * 危険を表します。
     */
    DANGER("danger");

    @JsonValue
    private final String name;

    public static AdmonitionType of(String name)
    {
        for (AdmonitionType type : values())
            if (type.getName().equals(name))
                return type;

        return null;
    }
}
