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
import org.kunlab.scenamatica.plugin.idea.ledger.models.LedgerReference;
import org.kunlab.scenamatica.plugin.idea.ledger.models.LedgerType;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.lang.ScenarioFile;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.models.ScenarioType;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.policy.MinecraftVersion;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.policy.ScenamaticaPolicyRetriever;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.policy.lang.ScenamaticaPolicy;
import org.kunlab.scenamatica.plugin.idea.utils.YAMLUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
            this.results = new ArrayList<>();
            return;
        }

        if (session.getUserData(KEY) == null)
            session.putUserData(KEY, this.results = new ArrayList<>());
        else
            this.results = session.getUserData(KEY);
    }

    private YAMLMapping getDocumentRoot()
    {
        List<YAMLDocument> docs = this.file.getDocuments();
        if (docs.isEmpty())
            return null;

        YAMLPsiElement topLevelElement = docs.get(0).getTopLevelValue();
        if (!(topLevelElement instanceof YAMLMapping mapping))
            return null;

        return mapping;
    }

    @Contract(" -> this")
    public LedgerScenarioResolver detailedResolve()
    {
        if (!this.results.isEmpty())
            return this;

        YAMLMapping root = this.getDocumentRoot();
        if (root == null)
            return this;

        this.checkScenarioFileMinecraftVersion(root);
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

    public List<ResolveResult> getErrors(@NotNull ResolveResult.InvalidCause... causes)
    {
        // causes の中身の数で処理を分岐して最適化を図る
        Predicate<? super ResolveResult> causeFilter;
        if (causes.length == 0)
            return Collections.emptyList();
        else if (causes.length == 1)
        {
            ResolveResult.InvalidCause cause = causes[0];
            causeFilter = c -> c.invalidCause == cause;
        }
        else
            causeFilter = cause -> {
                for (ResolveResult.InvalidCause c : causes)
                {
                    if (cause.invalidCause == c)
                        return true;
                }

                return false;
            };

        return new ArrayList<>(this.results).stream()
                .filter(result -> !result.isValid())
                .filter(causeFilter)
                .toList();
    }

    public List<ResolveResult> getActions()
    {
        return this.results.stream()
                .filter(ResolveResult::isActionBlockStart)
                .toList();
    }

    public ResolveResult getResultForElement(@NotNull PsiElement element)
    {
        return this.results.stream()
                .filter(result -> result.getElement() == element)
                .findFirst()
                .orElse(null);
    }

    private boolean checkScenarioFileMinecraftVersion(@NotNull YAMLMapping root)
    {
        YAMLKeyValue mcVersionPolicy = root.getKeyValueByKey("minecraft");
        if (mcVersionPolicy == null)
            return true;

        return this.checkMinecraftVersionRange(mcVersionPolicy.getValue(), null);
    }

    private boolean checkActionMinecraftVersionRange(@NotNull YAMLMapping scenarioMapping, @NotNull LedgerAction action)
    {
        YAMLKeyValue mcVersionPolicy = scenarioMapping.getKeyValueByKey("minecraft");
        if (mcVersionPolicy == null)
            return true;

        return this.checkMinecraftVersionRange(mcVersionPolicy.getValue(), action);
    }

    private boolean checkMinecraftVersionRange(@Nullable YAMLValue versionPolicy, @Nullable LedgerAction action)
    {
        if (versionPolicy == null)
            return true;

        if (!(versionPolicy instanceof YAMLMapping policyMapping))
            return true;  // 特に指定するのがめんどいので。

        YAMLKeyValue sinceValue = policyMapping.getKeyValueByKey("since");
        YAMLKeyValue untilValue = policyMapping.getKeyValueByKey("until");

        MinecraftVersion sinceParsed = sinceValue == null ? null: MinecraftVersion.fromString(YAMLUtils.getUnquotedValueText(sinceValue.getValue()));
        MinecraftVersion untilParsed = untilValue == null ? null: MinecraftVersion.fromString(YAMLUtils.getUnquotedValueText(untilValue.getValue()));

        // バージョン文字列が正しいものであるか？
        if (sinceValue != null && sinceParsed == MinecraftVersion.ANY)
        {
            this.registerInvalidVersionFormat(sinceValue, action);
            return false;
        }
        else if (untilValue != null && untilParsed == MinecraftVersion.ANY)
        {
            this.registerInvalidVersionFormat(untilValue, action);
            return false;
        }

        MinecraftVersion actualVersion = this.policy.getMinecraftVersion();
        if (actualVersion == null || actualVersion == MinecraftVersion.ANY)
            return true;

        if (!actualVersion.isInRange(sinceParsed, untilParsed))
        {
            PsiElement element = versionPolicy.getParent();
            if (!(element instanceof YAMLMapping mapping))
                return true;
            this.registerInvalidVersionRange(mapping, actualVersion, sinceParsed, untilParsed);
            return false;
        }

        return true;
    }

    private void registerInvalidVersionFormat(@NotNull YAMLKeyValue keyValue, @Nullable LedgerAction action)
    {
        this.registerInvalidResult(
                keyValue.getValue() == null ? keyValue: keyValue.getValue(),
                null,
                action,
                ResolveResult.InvalidCause.VALUE_CONSTRAINT_VIOLATION,
                ScenamaticerBundle.of(
                        "editor.inspections.invalidVersionFormat.title",
                        YAMLUtils.getUnquotedValueText(keyValue.getValue())
                )
        );
    }

    private void registerInvalidVersionRange(@NotNull YAMLMapping versionSpecifingMapping, @NotNull MinecraftVersion unavailableVersion, @Nullable MinecraftVersion versionSince, @Nullable MinecraftVersion versionUntil)
    {
        String description;
        if (versionSince == null)
        {
            assert versionUntil != null;
            description = ScenamaticerBundle.of(
                    "editor.inspections.unsupportedActionUsage.version.description.until",
                    versionUntil.toString(),
                    unavailableVersion
            );
        }
        else if (versionUntil == null)
            description = ScenamaticerBundle.of(
                    "editor.inspections.unsupportedActionUsage.version.description.since",
                    versionSince.toString(),
                    unavailableVersion
            );
        else
            description = ScenamaticerBundle.of(
                    "editor.inspections.unsupportedActionUsage.version.description.ranged",
                    versionSince.toString(),
                    versionUntil.toString(),
                    unavailableVersion
            );

        this.registerInvalidResult(
                versionSpecifingMapping,
                null,
                null,
                ResolveResult.InvalidCause.UNSUPPORTED_SERVER_VERSION,
                description
        );
    }

    private boolean processAction(@NotNull YAMLMapping current, @Nullable LedgerAction lastAction, @NotNull LedgerType currentType)
    {
        YAMLKeyValue actionID = current.getKeyValueByKey("action");
        YAMLKeyValue inputs = current.getKeyValueByKey("with");
        YAMLKeyValue usage = current.getKeyValueByKey("type");
        YAMLKeyValue mcVersionPolicy = current.getKeyValueByKey("minecraft");

        if (usage == null)
            return true;

        String usageString = YAMLUtils.getUnquotedValueText(usage.getValue());
        ScenarioType actionUsage = ScenarioType.of(usageString);
        if (actionUsage == null)
        {
            this.registerInvalidResultActionStart(
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
            this.registerInvalidResultActionStart(
                    current,
                    currentType,
                    lastAction,
                    ResolveResult.InvalidCause.UNKNOWN_ACTION,
                    "The action ID is not defined."
            );
            return false;
        }

        Optional<LedgerAction> actionOpt = this.ledgerManager.getActionByID(YAMLUtils.getUnquotedValueText(actionID.getValue()));
        if (actionOpt.isEmpty())
        {
            this.registerInvalidResultActionStart(
                    actionID,
                    currentType,
                    lastAction,
                    ResolveResult.InvalidCause.UNKNOWN_ACTION,
                    ScenamaticerBundle.of(
                            "editor.inspections.unknownAction.title",
                            YAMLUtils.getUnquotedValueText(actionID.getValue())
                    ),
                    actionUsage
            );

            return false;
        }

        LedgerAction action = actionOpt.get();
        if (!action.isApplicable(actionUsage))
        {
            this.registerInvalidResultActionStart(
                    current,
                    currentType,
                    lastAction,
                    ResolveResult.InvalidCause.ACTION_USAGE_VIOLATION,
                    ScenamaticerBundle.of(
                            "editor.inspections.unsupportedActionUsage.action.description.title",
                            action.getId(),
                            actionUsage.getDisplayName()
                    ),
                    actionUsage
            );
            return false;
        }

        this.registerAnchorResultActionStart(
                actionID,
                currentType,
                actionOpt.get(),
                actionUsage
        );

        if (!(mcVersionPolicy == null || this.checkActionMinecraftVersionRange(current, action)))
            return false;


        if (inputs == null || inputs.getValue() == null)
            return true;

        if (!(inputs.getValue() instanceof YAMLMapping mapping))
            return true;

        // 引数の型が正しいか？
        boolean result = this.resolveMapping(mapping, action, currentType, actionUsage, true);
        result &= this.checkActionInputUsageValid(action, actionUsage, mapping);

        return result;
    }

    private boolean checkActionInputUsageValid(@NotNull LedgerAction action, @NotNull ScenarioType usage, @NotNull YAMLMapping inputs)
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
                        inputs.getParent(),
                        null,
                        action,
                        ResolveResult.InvalidCause.ACTION_INPUT_MISSING_REQUIRED,
                        ScenamaticerBundle.of(
                                "editor.inspections.missingArgument.title",
                                key,
                                action.getId(),
                                usage.getDisplayName()
                        ),
                        usage
                );
                result = false;
            }

            // 許可されていない使用法でないか?
            if (!input.isAvailableFor(usage) && hasKey)
            {
                this.registerInvalidResult(
                        keyValue,
                        null,
                        action,
                        ResolveResult.InvalidCause.ACTION_INPUT_UNAVAILABLE_USAGE,
                        ScenamaticerBundle.of(
                                "editor.inspections.unsupportedArgumentUsage.title",
                                key,
                                action.getId(),
                                usage.getDisplayName()
                        ),
                        usage
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
                        keyValue,
                        null,
                        action,
                        ResolveResult.InvalidCause.ACTION_INPUT_REDUNDANT,
                        ScenamaticerBundle.of(
                                "editor.inspections.redundantArgument.title",
                                keyValue.getKeyText(),
                                action.getId(),
                                usage.getDisplayName()
                        ),
                        usage
                );
            }

            result = false;
        }

        return result;
    }

    private boolean resolveMapping(YAMLMapping current, @Nullable LedgerAction lastAction, @Nullable LedgerType lastType, @Nullable ScenarioType usage, boolean isActionProp)
    {
        LedgerType currentType = lastType == null ? this.ledgerManager.getPrimeType(): lastType;
        if (currentType == null)
            return false;
        else if (currentType.getId().equals("ScenarioStructure") && !isActionProp)
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

            if (view.getDetailedProperties().containsKey(key))
            {
                DetailedValue property = view.getDetailedProperties().get(key);
                result &= this.checkPropertyTypeMatch(property, keyValue, lastAction, usage);
            }
            else
            {
                // 存在しない（知らない）プロパティ。 action のプロパティは別口で処理済み。
                result = false;
                if (!isActionProp)
                    this.registerInvalidResult(
                            keyValue,
                            currentType,
                            lastAction,
                            ResolveResult.InvalidCause.UNKNOWN_PROPERTY,
                            ScenamaticerBundle.of(
                                    "editor.inspections.unknownProperty.title",
                                    keyValue.getKeyText(),
                                    currentType.getName()
                            ),
                            usage
                    );
            }
        }

        return result;
    }

    private void registerValidResult(@NotNull PsiElement element,
                                     @Nullable LedgerType type,
                                     @Nullable LedgerAction action,
                                     @Nullable ScenarioType usage)
    {
        this.results.add(new ResolveResult(element, getKeyTextRangeIfElementIsKV(element), true, null, null, type, action, usage, false));
    }

    private void registerInvalidResult(@NotNull PsiElement element,
                                       @Nullable LedgerType type,
                                       @Nullable LedgerAction action,
                                       @NotNull ResolveResult.InvalidCause cause,
                                       @Nullable String message)
    {
        this.results.add(new ResolveResult(element, getKeyTextRangeIfElementIsKV(element), false, cause, message, type, action, null, false));
    }

    private void registerInvalidResultActionStart(@NotNull PsiElement element,
                                                  @Nullable LedgerType type,
                                                  @Nullable LedgerAction action,
                                                  @NotNull ResolveResult.InvalidCause cause,
                                                  @Nullable String message
    )
    {
        this.results.add(new ResolveResult(element, getKeyTextRangeIfElementIsKV(element), false, cause, message, type, action, null, true));
    }

    private void registerTypeMismatchResult(@NotNull PsiElement element,
                                            @Nullable LedgerType type,
                                            @Nullable LedgerAction action,
                                            @Nullable ScenarioType usage)
    {
        this.results.add(new ResolveResult(
                element,
                element.getTextRange(),
                false,
                ResolveResult.InvalidCause.TYPE_MISMATCH,
                "The value does not match the type constraint.",
                type,
                action,
                usage,
                false
        ));
    }

    private void registerAnchorResultActionStart(@NotNull PsiElement element,
                                                 @Nullable LedgerType type,
                                                 @Nullable LedgerAction action,
                                                 @Nullable ScenarioType usage)
    {
        this.results.add(new ResolveResult(element, getKeyTextRangeIfElementIsKV(element), true, null, null, type, action, usage, true));
    }

    private void registerInvalidResult(@NotNull PsiElement element,
                                       @Nullable LedgerType type,
                                       @Nullable LedgerAction action,
                                       @NotNull ResolveResult.InvalidCause cause,
                                       @Nullable String message,
                                       @Nullable ScenarioType usage)
    {
        this.results.add(new ResolveResult(element, getKeyTextRangeIfElementIsKV(element), false, cause, message, type, action, usage, false));
    }

    private void registerInvalidResultActionStart(@NotNull PsiElement element,
                                                  @Nullable LedgerType type,
                                                  @Nullable LedgerAction action,
                                                  @NotNull ResolveResult.InvalidCause cause,
                                                  @Nullable String message,
                                                  @Nullable ScenarioType usage)
    {
        this.results.add(new ResolveResult(element, getKeyTextRangeIfElementIsKV(element), false, cause, message, type, action, usage, true));
    }

    private boolean checkPropertyTypeMatch(DetailedValue property, YAMLKeyValue keyValue, @Nullable LedgerAction lastAction, @Nullable ScenarioType usage)
    {
        boolean isValid = true;

        Optional<LedgerType> optPropertyType = this.ledgerManager.resolveReference(property.getType(), LedgerType.class);
        LedgerType propertyType;
        if (optPropertyType.isPresent())
            propertyType = optPropertyType.get();
        else if (PrimitiveType.isPrimitiveType(property.getType().getReferenceBody()))
            propertyType = PrimitiveType.fromString(property.getType().getReferenceBody());
        else
            return true;

        YAMLValue actualValue = keyValue.getValue();

        if (actualValue == null)
        {
            if (usage == null)
                return true;
            else
                return property.isRequiredOn(usage);
        }

        List<? extends YAMLPsiElement> targetValues = null;
        if (actualValue instanceof YAMLSequence sequence)
        {
            if (property.isArray())
                targetValues = sequence.getItems().stream()
                        .filter(yamlSeq -> yamlSeq.getChildren().length >= 1)
                        .map(yamlSeq -> yamlSeq.getChildren()[0])
                        .map(YAMLPsiElement.class::cast)
                        .toList();
            else
            {
                isValid = false;
                this.registerTypeMismatchResult(
                        actualValue,
                        propertyType,
                        lastAction,
                        usage
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
                        lastAction,
                        usage
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
                            lastAction,
                            usage
                    );
                }
                continue;
            }

            if (targetValue instanceof YAMLValue value)
            {
                // プリミティブ型の場合は特別に対応
                String typeID = property.getType().getReferenceBody();
                String mayError;
                if (typeID.startsWith("enums:")) // Enum の場合も特別に。
                {
                    Optional<LedgerType> enumType = this.ledgerManager.resolveReference(property.getType(), LedgerType.class);
                    if (enumType.isEmpty())
                        continue;
                    mayError = checkErrorEnumType(value, enumType.get());
                }
                else
                    mayError = checkErrorPrimitiveType(typeID, value, property);

                if (mayError != null)
                {
                    isValid = false;
                    this.registerInvalidResult(
                            value,
                            propertyType,
                            lastAction,
                            ResolveResult.InvalidCause.TYPE_MISMATCH,
                            mayError,
                            usage
                    );
                    continue;
                }
            }

            this.registerValidResult(targetValue, propertyType, lastAction, usage);
        }

        this.registerValidResult(keyValue, propertyType, lastAction, usage);
        return true;
    }

    private String checkErrorEnumType(YAMLValue actualValue, LedgerType currentType)
    {
        if (currentType.getEnums() == null)
            return null;

        YamlGenericValueAdapter adapter = new YamlGenericValueAdapter(actualValue);
        if (!adapter.isStringLiteral())
            return ScenamaticerBundle.of("editor.inspections.types.notATypeOf", currentType.getName());

        // LedgerType#getEnums() の Key は必ず Upper-Case であることが保証されている
        String value = YAMLUtils.getUnquotedValueText(actualValue).toUpperCase(Locale.ENGLISH);
        boolean isMatched;
        if (currentType.getName().equals("enums:EntityType"))
            isMatched = EntityTypeValidator.isValidEntityType(value);
        else
            isMatched = currentType.getEnums().keySet().stream()
                    .anyMatch(value::equalsIgnoreCase);
        
        if (isMatched)
            return null;

        return ScenamaticerBundle.of(
                "editor.inspections.types.notContains",
                currentType.getEnums().keySet().stream()
                        .map(e -> "\"" + e + "\"")
                        .collect(Collectors.joining(", "))
        );
    }

    private String checkErrorPrimitiveType(String primitiveName, YAMLValue actualValue, @Nullable DetailedValue detailed)
    {
        YamlGenericValueAdapter adapter = new YamlGenericValueAdapter(actualValue);
        String notATypeOf = ScenamaticerBundle.of("editor.inspections.types.notATypeOf", primitiveName);

        return switch (primitiveName)
        {
            case "integer", "long", "float", "double", "short", "byte" ->
            {
                if (!adapter.isNumberLiteral())
                    yield notATypeOf;
                else if (detailed == null)
                    yield null;

                Double value = parseNumberLiteral(adapter.getDelegate().getText());
                if (value == null)
                    yield notATypeOf;

                Number min = detailed.getMin() == null ? getMinimumValueOf(primitiveName): detailed.getMin();
                Number max = detailed.getMax() == null ? getMaximumValueOf(primitiveName): detailed.getMax();
                if (min == null && max == null)
                    yield null;
                else if (min == null)
                {
                    if (max.doubleValue() < value)
                        yield ScenamaticerBundle.of("editor.inspections.types.ranged.max", max);
                }
                else if (max == null)
                {
                    if (min.doubleValue() > value)
                        yield ScenamaticerBundle.of("editor.inspections.types.ranged.min", min);
                }
                else if (max.doubleValue() < value && min.doubleValue() > value)
                    yield ScenamaticerBundle.of("editor.inspections.types.ranged", min, max);

                yield null;
            }
            case "boolean" -> adapter.isBooleanLiteral() ? null: notATypeOf;
            case "string" ->
            {
                if (!adapter.isStringLiteral())
                    yield notATypeOf;
                else if (detailed == null)
                    yield null;

                if (detailed.getFormat() != null)
                {
                    String text = adapter.getDelegate().getText();
                    Pattern pattern = detailed.getFormat().getPattern();
                    if (!pattern.matcher(text).matches())
                        yield ScenamaticerBundle.of("editor.inspections.types.notMatched", pattern.toString());
                }

                if (!(detailed.getEnums() == null || detailed.getEnums().containsKey(adapter.getDelegate().getText())))
                    yield ScenamaticerBundle.of(
                            "editor.inspections.types.notContains",
                            detailed.getEnums().keySet().stream()
                                    .map(e -> "\"" + e + "\"")
                                    .collect(Collectors.joining(", "))
                    );


                if (detailed.getPattern() != null)
                {
                    String text = adapter.getDelegate().getText();
                    String patternStr = detailed.getPattern();
                    Pattern pattern = Pattern.compile(patternStr);
                    if (!pattern.matcher(text).matches())
                        yield ScenamaticerBundle.of("editor.inspections.types.notMatched", pattern.toString());
                }

                yield null;
            }
            default -> null; // プリミティブでない場合は無条件に合致
        };
    }

    private static Number getMaximumValueOf(String primitiveName)
    {
        return switch (primitiveName)
        {
            case "integer" -> Integer.MAX_VALUE;
            case "long" -> Long.MAX_VALUE;
            case "float" -> Float.MAX_VALUE;
            case "double" -> Double.MAX_VALUE;
            case "short" -> Short.MAX_VALUE;
            case "byte" -> Byte.MAX_VALUE;
            default -> null;
        };
    }

    private static Number getMinimumValueOf(String primitiveName)
    {
        return switch (primitiveName)
        {
            case "integer" -> Integer.MIN_VALUE;
            case "long" -> Long.MIN_VALUE;
            case "float" -> Float.MIN_VALUE;
            case "double" -> Double.MIN_VALUE;
            case "short" -> Short.MIN_VALUE;
            case "byte" -> Byte.MIN_VALUE;
            default -> null;
        };
    }

    private static TextRange getKeyTextRangeIfElementIsKV(@NotNull PsiElement element)
    {
        if (element instanceof YAMLKeyValue kv && kv.getKey() != null)
            return kv.getKey().getTextRange();
        return element.getTextRange();
    }

    private static ScenamaticaPolicy retrievePolicy(@NotNull ScenarioFile file)
    {
        return ScenamaticaPolicyRetriever.retrieveOrGuessPolicy(
                proposal -> proposal.getMinecraftVersion() != null,
                file.getProject(),
                file.getVirtualFile()
        );
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
        @Nullable
        ScenarioType usage;
        boolean isActionBlockStart;

        public enum InvalidCause
        {
            TYPE_MISMATCH,
            VALUE_CONSTRAINT_VIOLATION,
            UNSUPPORTED_SERVER_VERSION,  // UnsupportedMinecraftVersionInspector

            UNKNOWN_PROPERTY,
            UNKNOWN_ACTION,
            UNKNOWN_SCENARIO_TYPE,

            ACTION_USAGE_VIOLATION,  // UnsupportedActionUsageInspector
            ACTION_INPUT_MISSING_REQUIRED,  // MissingArgumentsInspector
            ACTION_INPUT_UNAVAILABLE_USAGE,
            ACTION_INPUT_REDUNDANT
        }
    }

    public static class PrimitiveType extends LedgerType
    {
        public static final PrimitiveType INTEGER = new PrimitiveType("integer", "Integer");
        public static final PrimitiveType LONG = new PrimitiveType("long", "Long");
        public static final PrimitiveType SHORT = new PrimitiveType("short", "Short");
        public static final PrimitiveType BYTE = new PrimitiveType("byte", "Byte");
        public static final PrimitiveType FLOAT = new PrimitiveType("float", "Float");
        public static final PrimitiveType DOUBLE = new PrimitiveType("double", "Double");
        public static final PrimitiveType BOOLEAN = new PrimitiveType("boolean", "Boolean");
        public static final PrimitiveType CHAR = new PrimitiveType("char", "Character");
        public static final PrimitiveType OBJECT = new PrimitiveType("object", "Object");

        private PrimitiveType(String id, String name)
        {
            super(LedgerReference.of("$ref:type:" + id), id, name, null, null, null, null, null, null, null);
        }

        public static boolean isPrimitiveType(String mayPrimitiveName)
        {
            return switch (mayPrimitiveName)
            {
                case "string", "integer", "long", "short", "byte", "float", "double", "boolean", "char", "object" ->
                        true;
                default -> false;
            };
        }

        public static PrimitiveType fromString(String primitiveName)
        {
            return switch (primitiveName)
            {
                case "integer" -> INTEGER;
                case "long" -> LONG;
                case "short" -> SHORT;
                case "byte" -> BYTE;
                case "float" -> FLOAT;
                case "double" -> DOUBLE;
                case "boolean" -> BOOLEAN;
                case "char" -> CHAR;
                case "object" -> OBJECT;
                default -> null;
            };
        }
    }
}
