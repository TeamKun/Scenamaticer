package org.kunlab.scenamatica.plugin.idea.ledger;

import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import lombok.Value;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLMapping;
import org.jetbrains.yaml.psi.YAMLPsiElement;
import org.jetbrains.yaml.psi.YAMLSequence;
import org.jetbrains.yaml.psi.YAMLValue;
import org.jetbrains.yaml.schema.YamlGenericValueAdapter;
import org.kunlab.scenamatica.plugin.idea.ScenamaticerBundle;
import org.kunlab.scenamatica.plugin.idea.ledger.models.DetailedValue;
import org.kunlab.scenamatica.plugin.idea.ledger.models.IDetailedPropertiesHolder;
import org.kunlab.scenamatica.plugin.idea.ledger.models.LedgerAction;
import org.kunlab.scenamatica.plugin.idea.ledger.models.LedgerStringType;
import org.kunlab.scenamatica.plugin.idea.ledger.models.LedgerType;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.lang.ScenarioFile;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.models.ScenarioType;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.policy.ScenamaticaPolicyRetriever;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.policy.lang.ScenamaticaPolicy;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public class LedgerScenarioResolver
{
    private static final Key<List<ResolveResult>> KEY = new Key<>("scenamatica.ledger.resolver");

    private final LedgerManager ledgerManager;
    private final ScenarioFile file;
    private final ScenamaticaPolicy policy;

    private final List<ResolveResult> results;

    private LedgerScenarioResolver(@NotNull LedgerManager ledgerManager, @NotNull ScenarioFile file, @Nullable LocalInspectionToolSession session)
    {
        this.ledgerManager = ledgerManager;
        this.file = file;
        this.policy = retrievePolicy(file);
        
        if (session == null)
        {
            this.results = List.of();
            return;
        }

        if (session.getUserData(KEY) == null)
            session.putUserData(KEY, this.results = List.of());
        else
            this.results = session.getUserData(KEY);
    }

    @Contract(" -> this")
    public LedgerScenarioResolver detailedResolve()
    {
        if (!this.results.isEmpty())
            return this;

        List<YAMLDocument> docs = this.file.getDocuments();
        if (docs.isEmpty())
            return this;

        // 最初のドキュメントのみ処理
        YAMLDocument targetDoc = docs.get(0);
        YAMLMapping root = (YAMLMapping) targetDoc.getTopLevelValue();
        if (docs.isEmpty())
            return this;

        this.resolveMapping(root);

        return this;
    }

    private boolean resolveMapping(YAMLMapping root)
    {
        return this.resolveMapping(root, null, null, null, false);
    }

    public List<ResolveResult> getResults()
    {
        return Collections.unmodifiableList(this.results);
    }

    public List<ResolveResult> getErrors()
    {
        return this.results.stream().filter(result -> !result.isValid()).toList();
    }

    public List<ResolveResult> getErrors(@NotNull ResolveResult.InvalidCause cause)
    {
        return this.results.stream()
                .filter(result -> !result.isValid())
                .filter(result -> result.getInvalidCause() == cause)
                .toList();
    }

    private boolean processAction(@NotNull YAMLMapping current, @Nullable LedgerAction lastAction, @NotNull LedgerType currentType)
    {
        YAMLKeyValue actionID = current.getKeyValueByKey("action");
        YAMLKeyValue inputs = current.getKeyValueByKey("with");
        YAMLKeyValue usage = current.getKeyValueByKey("type");

        if (usage == null)
            return true;

        String usageString = usage.getValueText();
        ScenarioType type = ScenarioType.of(usageString);
        if (type == null)
        {
            this.registerInvalidResult(
                    usage,
                    currentType,
                    lastAction,
                    ResolveResult.InvalidCause.UNKNOWN_SCENARIO_TYPE,
                    "The scenario type is not defined."
            );
            return false;
        }

        if (actionID == null)
        {
            this.registerInvalidResult(
                    current,
                    currentType,
                    lastAction,
                    ResolveResult.InvalidCause.UNKNOWN_ACTION,
                    "The action ID is not defined."
            );
            return false;
        }

        Optional<LedgerAction> actionOpt = this.ledgerManager.getActionByID(actionID.getValueText());
        if (actionOpt.isEmpty())
        {
            this.registerInvalidResult(
                    actionID,
                    currentType,
                    lastAction,
                    ResolveResult.InvalidCause.UNKNOWN_ACTION,
                    ScenamaticerBundle.of(
                            "editor.inspections.unknownAction.title",
                            actionID.getValueText()
                    )
            );

            return false;
        }

        LedgerAction action = actionOpt.get();
        if (!action.isApplicable(type))
        {
            this.registerInvalidResult(
                    current,
                    currentType,
                    lastAction,
                    ResolveResult.InvalidCause.ACTION_USAGE_VIOLATION,
                    ScenamaticerBundle.of(
                            "editor.inspections.unsupportedActionUsage.action.description.title",
                            action.getName(),
                            type.getDisplayName()
                    )
            );
            return false;
        }

        if (inputs == null || inputs.getValue() == null)
            return true;

        if (!(inputs.getValue() instanceof YAMLMapping mapping))
            return true;

        // 引数の型が正しいか？
        boolean result = this.resolveMapping(mapping, action, currentType, type, true);
        result &= this.checkActionInputUsageValiD(action, type, mapping);

        return result;
    }

    private boolean checkActionInputUsageValiD(@NotNull LedgerAction action, @NotNull ScenarioType usage, @NotNull YAMLMapping inputs)
    {
        boolean result = true;
        Map<String, LedgerAction.ActionInput> availableInputs = action.getInputs();

        Collection<YAMLKeyValue>/* = SmartList<> */ keyValues = inputs.getKeyValues();  // コピーされる。
        for (Map.Entry<String, LedgerAction.ActionInput> entry : availableInputs.entrySet())
        {
            String key = entry.getKey();
            LedgerAction.ActionInput input = entry.getValue();

            YAMLKeyValue keyValue = popValueByKey(keyValues, key);
            boolean hasKey = keyValue != null;

            // 必須入力が指定されているか？
            if (input.isRequiredOn(usage) && !hasKey)
            {
                this.registerInvalidResult(
                        inputs,
                        null,
                        action,
                        ResolveResult.InvalidCause.ACTION_INPUT_MISSING_REQUIRED,
                        ScenamaticerBundle.of(
                                "editor.inspections.missingArgument.title",
                                key,
                                action.getName(),
                                usage.getDisplayName()
                        )
                );
                result = false;
            }

            // 許可されていない使用法でないか?
            if (!input.isAvailableFor(usage) && hasKey)
            {
                this.registerInvalidResult(
                        findWarnTargetBy(keyValue),
                        null,
                        action,
                        ResolveResult.InvalidCause.ACTION_INPUT_UNAVAILABLE_USAGE,
                        ScenamaticerBundle.of(
                                "editor.inspections.redundantArgumentUsage.title",
                                key,
                                action.getName(),
                                usage.getDisplayName()
                        )
                );
                result = false;
            }
        }

        if (!keyValues.isEmpty())
        {
            // 余分な入力がある場合は警告を出す。
            for (YAMLKeyValue keyValue : keyValues)
            {
                this.registerInvalidResult(
                        findWarnTargetBy(keyValue),
                        null,
                        action,
                        ResolveResult.InvalidCause.ACTION_INPUT_UNAVAILABLE_USAGE,
                        ScenamaticerBundle.of(
                                "editor.inspections.redundantArgumentUsage.title",
                                keyValue.getKeyText(),
                                action.getName(),
                                usage.getDisplayName()
                        )
                );
            }

            result = false;
        }

        return result;
    }

    private boolean resolveMapping(YAMLMapping current, @Nullable LedgerAction lastAction, @Nullable LedgerType lastType, @Nullable ScenarioType usage, boolean isActionProp)
    {
        LedgerType currentType = lastType == null ? this.ledgerManager.getPrimeType(): lastType;
        if (currentType.getId().equals("ActionStructure") && !isActionProp)
            return this.processAction(current, lastAction, currentType);

        IDetailedPropertiesHolder view = isActionProp ? lastAction: currentType;
        assert view != null;

        if (view.getDetailedProperties() == null)
            return true;

        boolean result = true;
        Collection<YAMLKeyValue> keyValues = current.getKeyValues();
        for (YAMLKeyValue keyValue : keyValues)
        {
            String key = keyValue.getKeyText();
            YAMLValue value = keyValue.getValue();

            if (view.getDetailedProperties().containsKey(key))
            {
                DetailedValue property = view.getDetailedProperties().get(key);
                boolean propertyResult = this.checkPropertyTypeMatch(property, value, lastAction, usage);
                if (propertyResult)
                    this.registerValidResult(keyValue, currentType, lastAction);

                result &= propertyResult;
            }
            else
            {
                // 存在しない（知らない）プロパティ。 action のプロパティは別口で処理済み。
                result = false;
                if (!isActionProp)
                    this.registerInvalidResult(
                            findWarnTargetBy(keyValue),
                            currentType,
                            lastAction,
                            ResolveResult.InvalidCause.UNKNOWN_PROPERTY,
                            "The property is not defined in the type."
                    );
            }
        }

        return result;
    }

    private void registerValidResult(@NotNull PsiElement element, @Nullable LedgerType type, @Nullable LedgerAction action)
    {
        this.results.add(new ResolveResult(element, element.getTextRange(), true, null, null, type, action));
    }

    private void registerInvalidResult(@NotNull PsiElement element, @Nullable LedgerType type, @Nullable LedgerAction action, @NotNull ResolveResult.InvalidCause cause, @Nullable String message)
    {
        this.results.add(new ResolveResult(element, element.getTextRange(), false, cause, message, type, action));
    }

    private void registerTypeMismatchResult(@NotNull PsiElement element, @Nullable LedgerType type, @Nullable LedgerAction action)
    {
        this.results.add(new ResolveResult(
                element,
                element.getTextRange(),
                false,
                ResolveResult.InvalidCause.TYPE_MISMATCH,
                "The value does not match the type constraint.",
                type,
                action
        ));
    }

    private boolean checkPropertyTypeMatch(DetailedValue property, YAMLValue actualValue, @Nullable LedgerAction lastAction, @Nullable ScenarioType usage)
    {
        boolean isValid = true;

        LedgerType propertyType = this.ledgerManager.resolveReference(property.getType(), LedgerType.class)
                .orElseThrow(() -> new IllegalStateException("Failed to resolve the property type, ledger broken?"));

        List<? extends YAMLPsiElement> targetValues = null;
        if (actualValue instanceof YAMLSequence sequence)
        {
            if (property.isArray())
                targetValues = sequence.getItems();
            else
            {
                isValid = false;
                this.registerTypeMismatchResult(
                        actualValue,
                        propertyType,
                        lastAction
                );
            }
        }
        else
        {
            if (property.isArray())
            {
                isValid = false;
                this.registerTypeMismatchResult(
                        actualValue,
                        propertyType,
                        lastAction
                );
            }
            else
                targetValues = List.of(actualValue);
        }

        if (!isValid)
            return false;

        for (YAMLPsiElement targetValue : targetValues)
        {
            if (targetValue instanceof YAMLMapping)
            {
                isValid &= this.resolveMapping((YAMLMapping) targetValue, lastAction, propertyType, usage, false);
                continue;
            }
            else if (targetValue instanceof YAMLSequence)
            {
                if (!property.isArray())
                {
                    isValid = false;
                    this.registerTypeMismatchResult(
                            actualValue,
                            propertyType,
                            lastAction
                    );
                }
                continue;
            }

            assert targetValue instanceof YAMLValue;
            YAMLValue value = (YAMLValue) targetValue;

            // プリミティブ型の場合は特別に対応
            String typeID = propertyType.getId();
            if (!checkPrimitiveTypeMatch(typeID, value, property))
            {
                isValid = false;
                this.registerTypeMismatchResult(
                        actualValue,
                        propertyType,
                        lastAction
                );
                continue;
            }

            this.registerValidResult(targetValue, propertyType, lastAction);
        }

        return isValid;
    }

    private boolean checkPrimitiveTypeMatch(String primitiveName, YAMLValue actualValue, @Nullable DetailedValue detailed)
    {
        YamlGenericValueAdapter adapter = new YamlGenericValueAdapter(actualValue);

        return switch (primitiveName)
        {
            case "integer", "long", "float", "double", "short", "byte" ->
            {
                if (!adapter.isNumberLiteral())
                    yield false;
                else if (detailed != null)
                {
                    Double value = parseNumberLiteral(adapter.getDelegate().getText());
                    if (value == null)
                        yield false;

                    if (detailed.getMin() != null && value < detailed.getMin())
                        yield false;

                    yield detailed.getMax() == null || value <= detailed.getMax();
                }
                yield true;
            }
            case "boolean" -> adapter.isBooleanLiteral();
            case "string" ->
            {
                if (!adapter.isStringLiteral())
                    yield false;

                if (!(detailed instanceof LedgerStringType string))
                    yield true;

                if (string.getFormat() != null)
                {
                    String text = adapter.getDelegate().getText();
                    Pattern pattern = string.getFormat().getPattern();
                    if (!pattern.matcher(text).matches())
                        yield false;
                }

                if (!(string.getEnums() == null || string.getEnums().containsKey(adapter.getDelegate().getText())))
                    yield false;

                if (string.getPattern() != null)
                {
                    String text = adapter.getDelegate().getText();
                    Pattern pattern = string.getPattern();
                    if (!pattern.matcher(text).matches())
                        yield false;
                }

                yield true;
            }
            default -> true; // プリミティブでない場合は無条件に合致
        };
    }

    private static ScenamaticaPolicy retrievePolicy(@NotNull ScenarioFile file)
    {
        return ScenamaticaPolicyRetriever.retrieveOrGuessPolicy(
                proposal -> proposal.getMinecraftVersion() != null,
                file.getProject(),
                file.getVirtualFile()
        );
    }

    @NotNull
    private static PsiElement findWarnTargetBy(@NotNull YAMLKeyValue kv)
    {
        PsiElement key = kv.getKey();
        return key == null ? kv: key;
    }

    @Nullable
    private static YAMLKeyValue popValueByKey(@NotNull Collection<YAMLKeyValue> keyValues, @NotNull String key)
    {
        Optional<YAMLKeyValue> keyValue = keyValues.stream()
                .filter(kv -> kv.getKeyText().equals(key))
                .findFirst();

        if (keyValue.isPresent())
        {
            keyValues.remove(keyValue.get());
            return keyValue.get();
        }

        return null;
    }

    private static Double parseNumberLiteral(String literal)
    {
        try
        {
            return Double.parseDouble(literal);
        }
        catch (NumberFormatException e)
        {
            return null;
        }
    }

    public static LedgerScenarioResolver create(@NotNull LedgerManager ledgerManager, @NotNull ScenarioFile file, @NotNull LocalInspectionToolSession session)
    {
        return new LedgerScenarioResolver(ledgerManager, file, session);
    }

    public static LedgerScenarioResolver create(@NotNull LedgerManager ledgerManager, @NotNull ScenarioFile file)
    {
        return new LedgerScenarioResolver(ledgerManager, file, null);
    }

    @Value
    public static class ResolveResult
    {
        @NotNull
        PsiElement element;
        @NotNull
        TextRange range;

        boolean valid;
        InvalidCause invalidCause;
        String invalidMessage;

        @Nullable
        LedgerType type;
        @Nullable
        LedgerAction action;

        public enum InvalidCause
        {
            TYPE_MISMATCH,
            VALUE_CONSTRAINT_VIOLATION,

            UNKNOWN_PROPERTY,
            UNKNOWN_ACTION,
            UNKNOWN_SCENARIO_TYPE,

            ACTION_USAGE_VIOLATION,
            ACTION_UNKNOWN_INPUT,

            ACTION_INPUT_MISSING_REQUIRED,
            ACTION_INPUT_UNAVAILABLE_USAGE,
            ACTION_INPUT_REDUNDANT
        }
    }
}
