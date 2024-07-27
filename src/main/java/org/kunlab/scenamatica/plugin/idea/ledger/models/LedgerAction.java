package org.kunlab.scenamatica.plugin.idea.ledger.models;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.models.ScenarioType;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.policy.MinecraftVersion;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

@Value
@EqualsAndHashCode(callSuper = true)
public class LedgerAction extends AbstractLedgerContent implements IDetailedPropertiesHolder
{
    private static final String KEY_ID = "id";
    private static final String KEY_NAME = "name";
    private static final String KEY_SUPER_ACTION = "super";
    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_CATEGORY = "category";
    private static final String KEY_EVENTS = "events";
    private static final String KEY_EXECUTABLE = "executable";
    private static final String KEY_EXPECTABLE = "expectable";
    private static final String KEY_REQUIREABLE = "requireable";
    private static final String KEY_SUPPORTS_SINCE = "supports_since";
    private static final String KEY_SUPPORTS_UNTIL = "supports_until";
    private static final String KEY_INPUTS = "inputs";
    private static final String KEY_OUTPUTS = "outputs";
    private static final String KEY_ADMONITIONS = "admonitions";

    String id;
    String name;
    LedgerReference superAction;
    String description;
    LedgerReference category;
    LedgerReference[] events;
    Contract executable;
    Contract expectable;
    Contract requireable;
    MinecraftVersion supportsSince;
    MinecraftVersion supportsUntil;
    Map<String, ActionInput> inputs;
    Map<String, ActionOutput> outputs;
    LedgerAdmonition[] admonitions;

    public LedgerAction(LedgerReference reference, String id, String name, LedgerReference superAction, String description, LedgerReference category, LedgerReference[] events, Contract executable, Contract expectable, Contract requireable, MinecraftVersion supportsSince, MinecraftVersion supportsUntil, Map<String, ActionInput> inputs, Map<String, ActionOutput> outputs, LedgerAdmonition[] admonitions)
    {
        super(reference);
        this.id = id;
        this.name = name;
        this.superAction = superAction;
        this.description = description;
        this.category = category;
        this.events = events;
        this.executable = executable;
        this.expectable = expectable;
        this.requireable = requireable;
        this.supportsSince = supportsSince;
        this.supportsUntil = supportsUntil;
        this.inputs = inputs;
        this.outputs = outputs;
        this.admonitions = admonitions;
    }

    public LedgerAction()
    {
        this(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    @Override
    public Map<String, ? extends DetailedValue> getDetailedProperties()
    {
        return this.inputs;
    }

    public boolean isApplicable(@NotNull ScenarioType usage)
    {
        return switch (usage)
        {
            case EXECUTE -> this.executable.isAvailable();
            case EXPECT -> this.expectable.isAvailable();
            case REQUIRE -> this.requireable.isAvailable();
        };
    }

    public String getDescription(@Nullable ScenarioType usage)
    {
        if (usage == null)
            return this.description;

        return switch (usage)
        {
            case EXECUTE -> this.executable.getDescription();
            case EXPECT -> this.expectable.getDescription();
            case REQUIRE -> this.requireable.getDescription();
        };
    }

    @Value
    @AllArgsConstructor
    public static class ActionOutput
    {
        private static final String KEY_NAME = "name";
        private static final String KEY_DESCRIPTION = "description";
        private static final String KEY_TARGETS = "targets";
        private static final String KEY_TYPE = "type";
        private static final String KEY_SUPPORTS_SINCE = "supportsSince";
        private static final String KEY_SUPPORTS_UNTIL = "supportsUntil";
        private static final String KEY_MIN = "min";
        private static final String KEY_MAX = "max";
        private static final String KEY_INHERITEd_FROM = "inheritedFrom";
        private static final String KEY_ADMONITIONS = "admonitions";

        String name;
        String description;
        @JsonDeserialize(using = ScenarioTypesDeserializer.class)
        ScenarioType[] targets;
        LedgerReference type;
        MinecraftVersion supportsSince;
        MinecraftVersion supportsUntil;
        Double min;
        Double max;
        LedgerReference inheritedFrom;
        LedgerAdmonition[] admonitions;

        public ActionOutput()
        {
            this(null, null, null, null, null, null, null, null, null, null);
        }

        public static ActionOutput inherit(ActionOutput value, LedgerReference superAction)
        {
            return new ActionOutput(
                    value.name,
                    value.description,
                    value.targets,
                    value.type,
                    value.supportsSince,
                    value.supportsUntil,
                    value.min,
                    value.max,
                    superAction,
                    value.admonitions
            );
        }
    }

    @Value
    @AllArgsConstructor
    public static class ActionInput implements DetailedValue
    {
        private static final String KEY_NAME = "name";
        private static final String KEY_TYPE = "type";
        private static final String KEY_DESCRIPTION = "description";
        private static final String KEY_REQUIRED_ON = "requiredOn";
        private static final String KEY_AVAILABLE_FOR = "availableFor";
        private static final String KEY_SUPPORTS_SINCE = "supportsSince";
        private static final String KEY_SUPPORTS_UNTIL = "supportsUntil";
        private static final String KEY_ARRAY = "array";
        private static final String KEY_MIN = "min";
        private static final String KEY_MAX = "max";
        private static final String KEY_CONST_VALUE = "const";
        private static final String KEY_REQUIRES_ACTOR = "requiresActor";
        private static final String KEY_INHERITED_FROM = "inheritedFrom";
        private static final String KEY_ADMONITIONS = "admonitions";

        String name;
        LedgerReference type;
        String description;
        @JsonDeserialize(using = ScenarioTypesDeserializer.class)
        ScenarioType[] requiredOn;
        @JsonDeserialize(using = ScenarioTypesDeserializer.class)
        ScenarioType[] availableFor;
        MinecraftVersion supportsSince;
        MinecraftVersion supportsUntil;
        boolean array;
        Double min;
        Double max;
        Object constValue;
        boolean requiresActor;
        LedgerReference inheritedFrom;
        LedgerAdmonition[] admonitions;

        public ActionInput()
        {
            this(null, null, null, null, null, null, null, false, null, null, null, false, null, null);
        }

        public boolean isRequiredOn(ScenarioType type)
        {
            return this.requiredOn != null && Arrays.stream(this.requiredOn).anyMatch(t -> t == type);
        }

        public boolean isAvailableFor(ScenarioType type)
        {
            return this.availableFor == null || Arrays.stream(this.availableFor).anyMatch(t -> t == type);
        }

        public static ActionInput inherit(@NotNull ActionInput parent, @NotNull LedgerReference ref)
        {
            return new ActionInput(
                    parent.name,
                    parent.type,
                    parent.description,
                    parent.requiredOn,
                    parent.availableFor,
                    parent.supportsSince,
                    parent.supportsUntil,
                    parent.array,
                    parent.min,
                    parent.max,
                    parent.constValue,
                    parent.requiresActor,
                    ref,
                    parent.admonitions
            );
        }
    }

    @Value
    @JsonDeserialize(using = Contract.ContractDeserializer.class)
    public static class Contract
    {
        boolean available;
        String description;

        private Contract(boolean available, String description)
        {
            this.available = available;
            if (!(available || description == null))
                throw new IllegalArgumentException("Description must be null if the action is unavailable.");
            this.description = description;
        }

        public Object serialize()
        {
            if (!this.available)
                return false;

            return this.description == null ? true: this.description;
        }

        public static Contract ofAvailable(@NotNull String desc)
        {
            return new Contract(true, desc);
        }

        public static Contract ofUnavailable()
        {
            return new Contract(false, null);
        }

        public static class ContractDeserializer extends JsonDeserializer<Contract>
        {
            @Override
            public Contract deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException
            {
                if (parser.currentToken().isBoolean())
                {
                    boolean value = parser.getBooleanValue();
                    if (value)
                        throw new InvalidFormatException(parser, "Available must have a description", value, Contract.class);
                    return Contract.ofUnavailable();
                }

                return Contract.ofAvailable(parser.getValueAsString());
            }
        }
    }
}
