package org.kunlab.scenamatica.plugin.idea.ledger.models;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;

import java.util.regex.Pattern;

@Getter
public enum StringFormat
{
    DATE_TIME("date-time", "(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?([+-]\\d{2}:\\d{2}|Z)?)"),
    DATE("date", "\\d{4}-\\d{2}-\\d{2}"),
    TIME("time", "\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?([+-]\\d{2}:\\d{2}|Z)?"),
    DURATION("duration", "P(?:\\d+Y)?(?:\\d+M)?(?:\\d+D)?(?:T(?:\\d+H)?(?:\\d+M)?(?:\\d+S)?)?"),

    EMAIL("email", "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"),
    IDN_EMAIL("idn-email", "[\\p{L}0-9._%+-]+@[\\p{L}0-9.-]+\\.[a-zA-Z]{2,}"),

    HOSTNAME("hostname", "[a-zA-Z0-9.-]{1,253}"),
    IDN_HOSTNAME("idn-hostname", "[\\p{L}0-9.-]{1,253}"),

    IPV4("ipv4", "((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)"),
    IPV6("ipv6", "(?:[\\da-fA-F]{1,4}:){7}[\\da-fA-F]{1,4}"),

    URI("uri", "(https?|ftp|file)://[\\S]+"),
    URI_REFERENCE("uri-reference", "[\\S]+"),
    IRI("iri", "(https?|ftp|file)://[\\S\\p{L}]+"),
    IRI_REFERENCE("iri-reference", "[\\S\\p{L}]+"),

    UUID("uuid", "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[4][0-9a-fA-F]{3}-[89ab][0-9a-fA-F]{3}-[0-9a-fA-F]{12}");

    private final String value;
    private final Pattern pattern;

    StringFormat(String value, String patternString)
    {
        this.value = value;
        this.pattern = Pattern.compile("^" + patternString + "$");
    }

    @JsonDeserialize
    public static StringFormat fromString(String value)
    {
        if (value == null)
            return null;

        for (StringFormat format : StringFormat.values())
        {
            if (format.value.equals(value))
                return format;
        }

        return null;
    }
}

