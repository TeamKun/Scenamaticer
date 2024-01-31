package org.kunlab.scenamatica.plugin.idea.utils;

public class URLUtils
{
    public static String concat(String url1, String... url2)
    {
        if (url1.endsWith("/"))
            url1 = url1.substring(0, url1.length() - 1);

        StringBuilder builder = new StringBuilder(url1);
        for (String url : url2)
        {
            if (url.startsWith("/"))
                url = url.substring(1);

            builder.append("/").append(url);
        }

        return builder.toString();
    }
}
