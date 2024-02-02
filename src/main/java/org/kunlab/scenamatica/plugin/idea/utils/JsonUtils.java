package org.kunlab.scenamatica.plugin.idea.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class JsonUtils
{
    public static JsonObject mergeRecursive(JsonObject base, JsonObject patch)
    {
        JsonObject result = new JsonObject();
        for (String key : base.keySet())
        {
            if (patch.has(key))
            {
                if (base.get(key).isJsonObject() && patch.get(key).isJsonObject())
                    result.add(key, mergeRecursive(base.get(key).getAsJsonObject(), patch.get(key).getAsJsonObject()));
                else if (base.get(key).isJsonArray() && patch.get(key).isJsonArray())
                    result.add(key, mergeRecursive(base.get(key).getAsJsonArray(), patch.get(key).getAsJsonArray()));
                else
                    result.add(key, patch.get(key));
            }
            else
                result.add(key, base.get(key));
        }
        for (String key : patch.keySet())
        {
            if (!base.has(key))
                result.add(key, patch.get(key));
        }
        return result;
    }

    public static JsonArray mergeRecursive(JsonArray base, JsonArray patch)
    {
        JsonArray result = new JsonArray();
        for (int i = 0; i < base.size(); i++)
        {
            if (i < patch.size())
            {
                if (base.get(i).isJsonObject() && patch.get(i).isJsonObject())
                    result.add(mergeRecursive(base.get(i).getAsJsonObject(), patch.get(i).getAsJsonObject()));
                else if (base.get(i).isJsonArray() && patch.get(i).isJsonArray())
                    result.add(mergeRecursive(base.get(i).getAsJsonArray(), patch.get(i).getAsJsonArray()));
                else
                    result.add(patch.get(i));
            }
            else
                result.add(base.get(i));
        }

        for (int i = base.size(); i < patch.size(); i++)
            result.add(patch.get(i));

        return result;
    }
}
