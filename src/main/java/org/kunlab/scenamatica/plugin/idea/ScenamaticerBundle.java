package org.kunlab.scenamatica.plugin.idea;

import com.intellij.AbstractBundle;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;
import org.kunlab.scenamatica.plugin.idea.settings.ScenamaticerSettingsState;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.function.Supplier;

@Getter
public class ScenamaticerBundle extends AbstractBundle
{
    private static final ScenamaticerBundle INSTANCE = new ScenamaticerBundle();
    private final Locale locale;

    private ScenamaticerBundle()
    {
        super("messages.ScenamaticerBundle");
        this.locale = Locale.forLanguageTag(ScenamaticerSettingsState.getInstance().getLanguage());
    }

    @Override
    protected @NotNull ResourceBundle findBundle(@NotNull String pathToBundle, @NotNull ClassLoader loader, ResourceBundle.@NotNull Control control)
    {
        ResourceBundle base = super.findBundle(pathToBundle, loader, control);

        if (!this.locale.equals(Locale.ENGLISH))
        {
            String localizedPath = pathToBundle + "_" + this.locale.getLanguage();
            ResourceBundle localeBundle = super.findBundle(localizedPath, ScenamaticerPluginDisposable.class.getClassLoader(), control);
            if (base.equals(localeBundle))
                return base;

            setParent(localeBundle, base);
            return localeBundle;
        }
        return base;
    }

    private static void setParent(ResourceBundle localeBundle, ResourceBundle base)
    {
        try
        {
            Method method = ResourceBundle.class.getDeclaredMethod("setParent", ResourceBundle.class);
            method.setAccessible(true);
            MethodHandles.lookup().unreflect(method).bindTo(localeBundle).invoke(base);
        }
        catch (Throwable e)
        {
            throw new RuntimeException(e);
        }
    }

    public static Locale getCurrentLocale()
    {
        return INSTANCE.getLocale();
    }

    public static String of(@PropertyKey(resourceBundle = "messages.ScenamaticerBundle") String key, Object... params)
    {
        return INSTANCE.getMessage(key, params);
    }

    public static Supplier<String> lazy(@PropertyKey(resourceBundle = "messages.ScenamaticerBundle") String key, Object... params)
    {
        return INSTANCE.getLazyMessage(key, params);
    }
}
