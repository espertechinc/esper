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
package com.espertech.esper.common.internal.event.variant;

import com.espertech.esper.common.client.*;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonVariantStream;
import com.espertech.esper.common.client.meta.EventTypeMetadata;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.event.bean.core.BeanEventType;
import com.espertech.esper.common.internal.event.core.*;
import com.espertech.esper.common.internal.util.CollectionUtil;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.*;

/**
 * Event type for variant event streams.
 * <p>
 * Caches properties after having resolved a property via a resolution strategy.
 */
public class VariantEventType implements EventTypeSPI {
    private EventTypeMetadata metadata;
    private final EventType[] variants;
    private final boolean variantAny;
    private final VariantPropResolutionStrategy propertyResStrategy;
    private final Map<String, VariantPropertyDesc> propertyDesc;
    private final String[] propertyNames;
    private final EventPropertyDescriptor[] propertyDescriptors;
    private final Map<String, EventPropertyDescriptor> propertyDescriptorMap;
    private final VariantPropertyGetterCache variantPropertyGetterCache;
    private Map<String, EventPropertyGetter> propertyGetterCodegeneratedCache;

    /**
     * Ctor.
     *
     * @param variantSpec the variant specification
     * @param metadata    event type metadata
     */
    public VariantEventType(EventTypeMetadata metadata, VariantSpec variantSpec) {
        this.metadata = metadata;
        this.variants = variantSpec.getEventTypes();
        this.variantAny = variantSpec.getTypeVariance() == ConfigurationCommonVariantStream.TypeVariance.ANY;
        this.variantPropertyGetterCache = new VariantPropertyGetterCache(variantSpec.getEventTypes());
        if (variantAny) {
            this.propertyResStrategy = new VariantPropResolutionStrategyAny(this);
        } else {
            this.propertyResStrategy = new VariantPropResolutionStrategyDefault(this);
        }

        propertyDesc = new HashMap<String, VariantPropertyDesc>();

        for (EventType type : variants) {
            String[] properties = type.getPropertyNames();
            properties = CollectionUtil.copyAndSort(properties);
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

    public EventType[] getVariants() {
        return variants;
    }

    public Class getUnderlyingType() {
        return Object.class;
    }

    public String getName() {
        return metadata.getName();
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

    public VariantPropertyGetterCache getVariantPropertyGetterCache() {
        return variantPropertyGetterCache;
    }

    public EventPropertyGetter getGetter(String propertyName) {
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

        propertyGetterCodegeneratedCache.put(propertyName, getterSPI);
        return getterSPI;
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

    public EventPropertyWriterSPI getWriter(String propertyName) {
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

    public ExprValidationException equalsCompareType(EventType eventType) {
        return this == eventType ? null : new ExprValidationException("Variant types mismatch");
    }

    public EventBeanCopyMethodForge getCopyMethodForge(String[] properties) {
        return null;
    }

    public void setMetadataId(long publicId, long protectedId) {
        metadata = metadata.withIds(publicId, protectedId);
    }

    public Set<EventType> getDeepSuperTypesAsSet() {
        return Collections.emptySet();
    }

    public boolean isVariantAny() {
        return variantAny;
    }

    public void validateInsertedIntoEventType(EventType eventType) throws ExprValidationException {
        VariantEventTypeUtil.validateInsertedIntoEventType(eventType, this);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param theEvent event
     * @return event
     */
    public EventBean getValueAddEventBean(EventBean theEvent) {
        return new VariantEventBean(this, theEvent);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param event event
     * @return event type
     */
    public EventType eventTypeForNativeObject(Object event) {
        if (event == null) {
            throw new EPException("Null event object returned");
        }
        for (EventType variant : variants) {
            if (variant instanceof BeanEventType) {
                BeanEventType beanEventType = (BeanEventType) variant;
                if (JavaClassHelper.isSubclassOrImplementsInterface(event.getClass(), beanEventType.getUnderlyingType())) {
                    return variant;
                }
            }
        }
        throw new EPException("Failed to determine event type for event object of type '" + event.getClass() + "' for use with variant stream '" + getName() + "'");
    }
}
