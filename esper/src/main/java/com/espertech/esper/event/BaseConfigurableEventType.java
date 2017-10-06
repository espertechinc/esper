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
package com.espertech.esper.event;

import com.espertech.esper.client.*;
import com.espertech.esper.collection.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * EventType than can be supplied with a preconfigured list of properties getters (aka. explicit properties).
 *
 * @author pablo
 */
public abstract class BaseConfigurableEventType implements EventTypeSPI {

    private final static Logger log = LoggerFactory.getLogger(BaseConfigurableEventType.class);

    private EventAdapterService eventAdapterService;
    private final int eventTypeId;
    private final EventTypeMetadata metadata;
    private Class underlyngType;
    private EventPropertyDescriptor[] propertyDescriptors;
    private String[] propertyNames;
    private Map<String, Pair<ExplicitPropertyDescriptor, FragmentEventType>> propertyFragmentTypes;
    private Map<String, EventPropertyGetter> propertyGetterCodegeneratedCache;

    /**
     * Getters for each known property.
     */
    protected Map<String, EventPropertyGetterSPI> propertyGetters;

    /**
     * Descriptors for each known property.
     */
    protected Map<String, EventPropertyDescriptor> propertyDescriptorMap;

    /**
     * Ctor.
     *
     * @param underlyngType       is the underlying type returned by the event type
     * @param metadata            event type metadata
     * @param eventAdapterService for dynamic event type creation
     * @param eventTypeId         type id
     */
    protected BaseConfigurableEventType(EventAdapterService eventAdapterService, EventTypeMetadata metadata, int eventTypeId, Class underlyngType) {
        this.eventTypeId = eventTypeId;
        this.eventAdapterService = eventAdapterService;
        this.metadata = metadata;
        this.underlyngType = underlyngType;
    }

    public int getEventTypeId() {
        return eventTypeId;
    }

    /**
     * Subclasses must implement this and supply a getter to a given property.
     *
     * @param property is the property expression
     * @return getter for property
     */
    protected abstract EventPropertyGetterSPI doResolvePropertyGetter(String property);

    /**
     * Subclasses must implement this and return a type for a property.
     *
     * @param property is the property expression
     * @return property type
     */
    protected abstract Class doResolvePropertyType(String property);

    /**
     * Subclasses must implement this and return a fragment type for a property.
     *
     * @param property is the property expression
     * @return fragment property type
     */
    protected abstract FragmentEventType doResolveFragmentType(String property);

    public String getName() {
        return metadata.getPrimaryName();
    }

    /**
     * Returns the event adapter service.
     *
     * @return event adapter service
     */
    public EventAdapterService getEventAdapterService() {
        return eventAdapterService;
    }

    /**
     * Sets explicit properties using a map of event property name and getter instance for each property.
     *
     * @param explicitProperties property descriptors for explicit properties
     */
    protected void initialize(List<ExplicitPropertyDescriptor> explicitProperties) {
        propertyGetters = new HashMap<String, EventPropertyGetterSPI>();
        propertyDescriptors = new EventPropertyDescriptor[explicitProperties.size()];
        propertyNames = new String[explicitProperties.size()];
        propertyDescriptorMap = new HashMap<String, EventPropertyDescriptor>();
        propertyFragmentTypes = new HashMap<String, Pair<ExplicitPropertyDescriptor, FragmentEventType>>();

        int count = 0;
        for (ExplicitPropertyDescriptor explicit : explicitProperties) {
            propertyNames[count] = explicit.getDescriptor().getPropertyName();
            propertyGetters.put(explicit.getDescriptor().getPropertyName(), explicit.getGetter());
            EventPropertyDescriptor desc = explicit.getDescriptor();
            propertyDescriptors[count] = desc;
            propertyDescriptorMap.put(desc.getPropertyName(), desc);

            if (explicit.getOptionalFragmentTypeName() != null) {
                propertyFragmentTypes.put(explicit.getDescriptor().getPropertyName(), new Pair<ExplicitPropertyDescriptor, FragmentEventType>(explicit, null));
            }

            if (!desc.isFragment()) {
                propertyFragmentTypes.put(explicit.getDescriptor().getPropertyName(), null);
            }
            count++;
        }
    }

    public Class getPropertyType(String propertyExpression) {
        EventPropertyDescriptor desc = propertyDescriptorMap.get(propertyExpression);
        if (desc != null) {
            return desc.getPropertyType();
        }

        return doResolvePropertyType(propertyExpression);
    }

    public Class getUnderlyingType() {
        return underlyngType;
    }

    public EventPropertyGetterSPI getGetterSPI(String propertyExpression) {
        EventPropertyGetterSPI getter = propertyGetters.get(propertyExpression);
        if (getter != null) {
            return getter;
        }

        return doResolvePropertyGetter(propertyExpression);
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

    public EventPropertyGetterMapped getGetterMapped(String mappedProperty) {
        EventPropertyGetterMappedSPI getter = getGetterMappedSPI(mappedProperty);
        if (getter == null) {
            return null;
        }
        if (!eventAdapterService.getEngineImportService().isCodegenEventPropertyGetters()) {
            return getter;
        }
        return eventAdapterService.getEngineImportService().codegenGetter(getter, metadata.getPublicName(), mappedProperty);
    }

    public EventPropertyGetterMappedSPI getGetterMappedSPI(String mappedProperty) {
        EventPropertyGetter getter = getGetter(mappedProperty);
        if (getter instanceof EventPropertyGetterMappedSPI) {
            return (EventPropertyGetterMappedSPI) getter;
        }
        return null;
    }

    public EventPropertyGetterIndexedSPI getGetterIndexedSPI(String propertyName) {
        return null;
    }

    public EventPropertyGetterIndexedSPI getGetterIndexed(String indexedProperty) {
        return null;
    }

    public synchronized FragmentEventType getFragmentType(String property) {
        Pair<ExplicitPropertyDescriptor, FragmentEventType> pair = propertyFragmentTypes.get(property);
        if (pair == null) {
            if (propertyFragmentTypes.containsKey(property)) {
                return null;
            }
            return doResolveFragmentType(property);
        }

        // if a type is assigned, use that
        if (pair.getSecond() != null) {
            return pair.getSecond();
        }

        // resolve event type
        EventType existingType = eventAdapterService.getExistsTypeByName(pair.getFirst().getOptionalFragmentTypeName());
        if (!(existingType instanceof BaseConfigurableEventType)) {
            log.warn("Type configured for fragment event property '" + property + "' by name '" + pair.getFirst() + "' could not be found");
            return null;
        }

        FragmentEventType fragmentType = new FragmentEventType(existingType, pair.getFirst().isFragmentArray(), false);
        pair.setSecond(fragmentType);
        return fragmentType;
    }

    public String[] getPropertyNames() {
        return propertyNames;
    }

    public boolean isProperty(String property) {
        return getGetter(property) != null;
    }

    public EventPropertyDescriptor[] getPropertyDescriptors() {
        return propertyDescriptors;
    }

    public EventTypeMetadata getMetadata() {
        return metadata;
    }

    public EventPropertyDescriptor getPropertyDescriptor(String propertyName) {
        return propertyDescriptorMap.get(propertyName);
    }
}
