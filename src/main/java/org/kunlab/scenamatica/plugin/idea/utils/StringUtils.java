package org.kunlab.scenamatica.plugin.idea.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils
{
    private static final Pattern CAMEL_CASE_PATTERN = Pattern.compile("([a-z])([A-Z])");

    public static String toKebabCase(String string)
    {
        Matcher matcher = CAMEL_CASE_PATTERN.matcher(string);
        StringBuilder builder = new StringBuilder();
        while (matcher.find())
            matcher.appendReplacement(builder, matcher.group(1) + "-" + matcher.group(2).toLowerCase());

        matcher.appendTail(builder);
        return builder.toString().toLowerCase()
                .replace(" ", "-")
                .replace("_", "-");
    }
}
