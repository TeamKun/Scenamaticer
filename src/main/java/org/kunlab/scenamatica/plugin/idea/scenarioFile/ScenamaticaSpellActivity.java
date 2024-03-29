package org.kunlab.scenamatica.plugin.idea.scenarioFile;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.spellchecker.SpellCheckerManager;
import com.intellij.spellchecker.dictionary.Dictionary;
import com.intellij.spellchecker.engine.SpellCheckerEngine;
import com.intellij.util.containers.ArrayListSet;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Set;

public class ScenamaticaSpellActivity implements ProjectActivity
{
    private static final Dictionary SCENAMATICA_DICTIONARY = new ScenamaticaDictionary();

    @Nullable
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation)
    {
        SpellCheckerEngine spellCheckerEngine = SpellCheckerManager.getInstance(project).getSpellChecker();
        if (!(spellCheckerEngine == null || spellCheckerEngine.isDictionaryLoad(SCENAMATICA_DICTIONARY.getName())))
            spellCheckerEngine.addDictionary(SCENAMATICA_DICTIONARY);

        return null;
    }

    private static class ScenamaticaDictionary implements Dictionary
    {
        private static final Set<String> KEYWORDS;

        static
        {
            ArrayListSet<String> keywords = new ArrayListSet<>();
            keywords.add("scenamatica");
            keywords.add("GameMode".toLowerCase());
            keywords.add("NamespacedKey".toLowerCase());
            keywords.add("RunIF".toLowerCase());

            KEYWORDS = Collections.unmodifiableSet(keywords);
        }

        @Override
        public @NotNull String getName()
        {
            return "Scenamatica Dictionary";
        }

        @Override
        public @Nullable Boolean contains(@NotNull String s)
        {
            return KEYWORDS.contains(s);
        }

        @Override
        public @NotNull Set<String> getWords()
        {
            return KEYWORDS;
        }
    }
}
