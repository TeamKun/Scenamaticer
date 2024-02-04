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

        String result = builder.toString().toLowerCase();
        if (Character.isUpperCase(string.charAt(0)))
            result = Character.toLowerCase(string.charAt(0)) + result.substring(1);

        result = result.replaceAll("([A-Z]+)", "-$1").toLowerCase();

        return result.replace(" ", "-")
                .replace("_", "-");
    }
}
