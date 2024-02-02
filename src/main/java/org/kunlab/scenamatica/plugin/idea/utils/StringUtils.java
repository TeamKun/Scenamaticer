package org.kunlab.scenamatica.plugin.idea.utils;

public class StringUtils
{
    public static String toKebabCase(String string)
    {
        return string.replaceAll("([a-z0-9])([A-Z])", "$1-$2").toLowerCase();
    }
}
