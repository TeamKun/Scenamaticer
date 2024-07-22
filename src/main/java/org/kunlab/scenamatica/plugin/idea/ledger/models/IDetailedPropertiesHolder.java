package org.kunlab.scenamatica.plugin.idea.ledger.models;

import java.util.Map;

public interface IDetailedPropertiesHolder
{
    Map<String, ? extends DetailedValue> getDetailedProperties();
}
