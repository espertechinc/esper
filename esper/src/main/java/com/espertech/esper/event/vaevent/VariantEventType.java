/*
 ***************************************************************************************
 *  Copyright (C) 2006 EsperTech, Inc. All rights reserved.                            *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 ***************************************************************************************
 */
package com.espertech.esper.event.vaevent;

import com.espertech.esper.client.*;
import com.espertech.esper.event.*;
import com.espertech.esper.util.JavaClassHelper;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Event type for variant event streams.
 * <p>
 * Caches properties after having resolved a property via a resolution strategy.
 */
public class VariantEventType implements EventTypeSPI {
    private final EventAdapterService eventAdapterService;
    private final EventTypeMetadata metadata;
    private final EventType[] variants;
    private final VariantPropResolutionStrategy propertyResStrategy;
    private final Map<String, VariantPropertyDesc> propertyDesc;
    private final String[] propertyNames;
    private final EventPropertyDescriptor[] propertyDescriptors;
    private final Map<String, EventPropertyDescriptor> propertyDescriptorMap;
    private final int eventTypeId;
    private final ConfigurationVariantStream config;
    private Map<String, EventPropertyGetter> propertyGetterCodegeneratedCache;

    /**
     * Ctor.
     *
     * @param variantSpec         the variant specification
     * @param propertyResStrategy stragegy for resolving properties
     * @param eventAdapterService event adapter service
     * @param metadata            event type metadata
     * @param eventTypeId         type id
     * @param config              configs
     */
    public VariantEventType(EventAdapterService eventAdapterService, EventTypeMetadata metadata, int eventTypeId, VariantSpec variantSpec, VariantPropResolutionStrategy propertyResStrategy, ConfigurationVariantStream config) {
        this.eventAdapterService = eventAdapterService;
        this.metadata = metadata;
        this.eventTypeId = eventTypeId;
        this.variants = variantSpec.getEventTypes();
        this.propertyResStrategy = propertyResStrategy;
        this.config = config;

        propertyDesc = new HashMap<String, VariantPropertyDesc>();

        for (EventType type : variants) {
            String[] properties = type.getPropertyNames();
            properties = PropertyUtility.copyAndSort(properties);
            for (String property : properties) {
                if (!propertyDesc.containsKey(property)) {
                    findProperty(property);
                }
            }
        }

        Set<String> propertyNameKeySet = propertyDesc.keySet();
        propertyNames = propertyNameKeySet.toArray(new String[propertyNameKeySet.size()]);

        // for each of the properties in each type, attempt to load the property to build a property list
        propertyDescriptors = new EventPropertyDescriptor[propertyDesc.size()];
        propertyDescriptorMap = new HashMap<String, EventPropertyDescriptor>();
        int count = 0;
        for (Map.Entry<String, VariantPropertyDesc> desc : propertyDesc.entrySet()) {
            Class type = desc.getValue().getPropertyType();
            EventPropertyDescriptor descriptor = new EventPropertyDescriptor(desc.getKey(), type, null, false, false, false, false, JavaClassHelper.isFragmentableType(desc.getValue().getPropertyType()));
            propertyDescriptors[count++] = descriptor;
            propertyDescriptorMap.put(desc.getKey(), descriptor);
        }
    }

    public String getStartTimestampPropertyName() {
        return null;
    }

    public String getEndTimestampPropertyName() {
        return null;
    }

    public Class getPropertyType(String property) {
        VariantPropertyDesc entry = propertyDesc.get(property);
        if (entry != null) {
            return entry.getPropertyType();
        }
        entry = findProperty(property);
        if (entry != null) {
            return entry.getPropertyType();
        }
        return null;
    }

    public Class getUnderlyingType() {
        return Object.class;
    }

    public String getName() {
        return metadata.getPublicName();
    }

    public int getEventTypeId() {
        return eventTypeId;
    }

    public ConfigurationVariantStream getConfig() {
        return config;
    }

    public EventPropertyGetterSPI getGetterSPI(String property) {
        VariantPropertyDesc entry = propertyDesc.get(property);
        if (entry != null) {
            return entry.getGetter();
        }
        entry = findProperty(property);
        if (entry != null) {
            return entry.getGetter();
        }
        return null;
    }

    public EventPropertyGetter getGetter(String propertyName) {
        if (!eventAdapterService.getEngineImportService().isCodegenEventPropertyGetters()) {
            return getGetterSPI(propertyName);
        }
        if (propertyGetterCodegeneratedCache == null) {
            propertyGetterCodegeneratedCache = new HashMap<>();
        }

        EventPropertyGetter getter = propertyGetterCodegeneratedCache.get(propertyName);
        if (getter != null) {
            return getter;
        }

        EventPropertyGetterSPI getterSPI = getGetterSPI(propertyName);
        if (getterSPI == null) {
            return null;
        }

        EventPropertyGetter getterCode = eventAdapterService.getEngineImportService().codegenGetter(getterSPI, metadata.getPublicName(), propertyName);
        propertyGetterCodegeneratedCache.put(propertyName, getterCode);
        return getterCode;
    }

    public String[] getPropertyNames() {
        return propertyNames;
    }

    public boolean isProperty(String property) {
        VariantPropertyDesc entry = propertyDesc.get(property);
        if (entry != null) {
            return entry.isProperty();
        }
        entry = findProperty(property);
        if (entry != null) {
            return entry.isProperty();
        }
        return false;
    }

    public EventType[] getSuperTypes() {
        return null;
    }

    public Iterator<EventType> getDeepSuperTypes() {
        return null;
    }

    private VariantPropertyDesc findProperty(String propertyName) {
        VariantPropertyDesc desc = propertyResStrategy.resolveProperty(propertyName, variants);
        if (desc != null) {
            propertyDesc.put(propertyName, desc);
        }
        return desc;
    }

    public EventTypeMetadata getMetadata() {
        return metadata;
    }

    public EventPropertyDescriptor[] getPropertyDescriptors() {
        return propertyDescriptors;
    }

    public EventPropertyDescriptor getPropertyDescriptor(String propertyName) {
        return propertyDescriptorMap.get(propertyName);
    }

    public FragmentEventType getFragmentType(String property) {
        return null;
    }

    public EventPropertyWriter getWriter(String propertyName) {
        return null;
    }

    public EventPropertyDescriptor getWritableProperty(String propertyName) {
        return null;
    }

    public EventPropertyDescriptor[] getWriteableProperties() {
        return new EventPropertyDescriptor[0];
    }

    public EventBeanCopyMethod getCopyMethod(String[] properties) {
        return null;
    }

    public EventBeanWriter getWriter(String[] properties) {
        return null;
    }

    public EventBeanReader getReader() {
        return null;
    }

    public EventPropertyGetterMapped getGetterMapped(String mappedProperty) {
        return null;
    }

    public EventPropertyGetterMappedSPI getGetterMappedSPI(String mappedProperty) {
        return null;
    }

    public EventPropertyGetterIndexed getGetterIndexed(String indexedProperty) {
        return null;
    }

    public EventPropertyGetterIndexedSPI getGetterIndexedSPI(String propertyName) {
        return null;
    }

    public boolean equalsCompareType(EventType eventType) {
        return this == eventType;
    }
}
