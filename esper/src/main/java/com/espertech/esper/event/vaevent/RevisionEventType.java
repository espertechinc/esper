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
import com.espertech.esper.epl.parse.ASTUtil;
import com.espertech.esper.event.*;
import com.espertech.esper.event.bean.BeanEventType;
import com.espertech.esper.event.property.*;
import com.espertech.esper.util.JavaClassHelper;
import com.espertech.esper.util.StringValue;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Event type of revision events.
 */
public class RevisionEventType implements EventTypeSPI {
    private final EventTypeMetadata metadata;
    private final int eventTypeId;
    private String[] propertyNames;
    private EventPropertyDescriptor[] propertyDescriptors;
    private Map<String, EventPropertyDescriptor> propertyDescriptorMap;
    private Map<String, RevisionPropertyTypeDesc> propertyDesc;
    private EventAdapterService eventAdapterService;
    private Map<String, EventPropertyGetter> propertyGetterCodegeneratedCache;

    /**
     * Ctor.
     *
     * @param propertyDesc        describes each properties type
     * @param eventAdapterService for nested property handling
     * @param metadata            - event type metadata
     * @param eventTypeId         type id
     */
    public RevisionEventType(EventTypeMetadata metadata, int eventTypeId, Map<String, RevisionPropertyTypeDesc> propertyDesc, EventAdapterService eventAdapterService) {
        this.metadata = metadata;
        this.eventTypeId = eventTypeId;
        this.propertyDesc = propertyDesc;
        Set<String> keys = propertyDesc.keySet();
        propertyNames = keys.toArray(new String[keys.size()]);
        this.eventAdapterService = eventAdapterService;

        propertyDescriptors = new EventPropertyDescriptor[propertyDesc.size()];
        propertyDescriptorMap = new HashMap<String, EventPropertyDescriptor>();
        int count = 0;
        for (Map.Entry<String, RevisionPropertyTypeDesc> desc : propertyDesc.entrySet()) {
            Class type = (Class) desc.getValue().getPropertyType();
            EventPropertyDescriptor descriptor = new EventPropertyDescriptor(desc.getKey(), type, null, false, false, false, false, JavaClassHelper.isFragmentableType(type));
            propertyDescriptors[count] = descriptor;
            propertyDescriptorMap.put(desc.getKey(), descriptor);
            count++;
        }
    }

    public int getEventTypeId() {
        return eventTypeId;
    }

    public String getStartTimestampPropertyName() {
        return null;
    }

    public String getEndTimestampPropertyName() {
        return null;
    }

    public EventPropertyGetterSPI getGetterSPI(String propertyName) {
        RevisionPropertyTypeDesc desc = propertyDesc.get(propertyName);
        if (desc != null) {
            return desc.getRevisionGetter();
        }

        // dynamic property names note allowed
        if (propertyName.indexOf('?') != -1) {
            return null;
        }

        // see if this is a nested property
        int index = StringValue.unescapedIndexOfDot(propertyName);
        if (index == -1) {
            Property prop = PropertyParser.parseAndWalkLaxToSimple(propertyName);
            if (prop instanceof SimpleProperty) {
                // there is no such property since it wasn't found earlier
                return null;
            }
            String atomic = null;
            if (prop instanceof IndexedProperty) {
                IndexedProperty indexedprop = (IndexedProperty) prop;
                atomic = indexedprop.getPropertyNameAtomic();
            }
            if (prop instanceof MappedProperty) {
                MappedProperty indexedprop = (MappedProperty) prop;
                atomic = indexedprop.getPropertyNameAtomic();
            }
            desc = propertyDesc.get(atomic);
            if (desc == null) {
                return null;
            }
            if (!(desc.getPropertyType() instanceof Class)) {
                return null;
            }
            Class nestedClass = (Class) desc.getPropertyType();
            BeanEventType complexProperty = (BeanEventType) eventAdapterService.addBeanType(nestedClass.getName(), nestedClass, false, false, false);
            return prop.getGetter(complexProperty, eventAdapterService);
        }

        // Map event types allow 2 types of properties inside:
        //   - a property that is a Java object is interrogated via bean property getters and BeanEventType
        //   - a property that is a Map itself is interrogated via map property getters

        // Take apart the nested property into a map key and a nested value class property name
        String propertyMap = ASTUtil.unescapeDot(propertyName.substring(0, index));
        String propertyNested = propertyName.substring(index + 1, propertyName.length());

        desc = propertyDesc.get(propertyMap);
        if (desc == null) {
            return null;  // prefix not a known property
        }

        // only nested classes supported for revision event types since deep property information not currently exposed by EventType
        if (desc.getPropertyType() instanceof Class) {
            // ask the nested class to resolve the property
            Class simpleClass = (Class) desc.getPropertyType();
            EventTypeSPI nestedEventType = (EventTypeSPI) eventAdapterService.addBeanType(simpleClass.getName(), simpleClass, false, false, false);
            EventPropertyGetterSPI nestedGetter = nestedEventType.getGetterSPI(propertyNested);
            if (nestedGetter == null) {
                return null;
            }

            // construct getter for nested property
            return new RevisionNestedPropertyGetter(desc.getRevisionGetter(), nestedGetter, eventAdapterService);
        } else {
            return null;
        }
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

    public String getName() {
        return metadata.getPublicName();
    }

    public Class getPropertyType(String propertyName) {
        RevisionPropertyTypeDesc desc = propertyDesc.get(propertyName);
        if (desc != null) {
            if (desc.getPropertyType() instanceof Class) {
                return (Class) desc.getPropertyType();
            }
            return null;
        }

        // dynamic property names note allowed
        if (propertyName.indexOf('?') != -1) {
            return null;
        }

        // see if this is a nested property
        int index = StringValue.unescapedIndexOfDot(propertyName);
        if (index == -1) {
            return null;
        }

        // Map event types allow 2 types of properties inside:
        //   - a property that is a Java object is interrogated via bean property getters and BeanEventType
        //   - a property that is a Map itself is interrogated via map property getters

        // Take apart the nested property into a map key and a nested value class property name
        String propertyMap = ASTUtil.unescapeDot(propertyName.substring(0, index));
        String propertyNested = propertyName.substring(index + 1, propertyName.length());

        desc = propertyDesc.get(propertyMap);
        if (desc == null) {
            return null;  // prefix not a known property
        } else if (desc.getPropertyType() instanceof Class) {
            Class simpleClass = (Class) desc.getPropertyType();
            EventType nestedEventType = eventAdapterService.addBeanType(simpleClass.getName(), simpleClass, false, false, false);
            return nestedEventType.getPropertyType(propertyNested);
        } else {
            return null;
        }
    }

    public Class getUnderlyingType() {
        return RevisionEventType.class;
    }

    public String[] getPropertyNames() {
        return propertyNames;
    }

    public boolean isProperty(String property) {
        return getPropertyType(property) != null;
    }

    public EventType[] getSuperTypes() {
        return null;
    }

    public Iterator<EventType> getDeepSuperTypes() {
        return null;
    }

    public EventTypeMetadata getMetadata() {
        return metadata;
    }

    public EventPropertyDescriptor[] getPropertyDescriptors() {
        return propertyDescriptors;
    }

    public FragmentEventType getFragmentType(String property) {
        return null;
    }

    public EventPropertyDescriptor getPropertyDescriptor(String propertyName) {
        return propertyDescriptorMap.get(propertyName);
    }

    public EventPropertyWriter getWriter(String propertyName) {
        return null;
    }

    public EventPropertyDescriptor[] getWriteableProperties() {
        return new EventPropertyDescriptor[0];
    }

    public EventBeanCopyMethod getCopyMethod(String[] properties) {
        return null;
    }

    public EventPropertyDescriptor getWritableProperty(String propertyName) {
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

    public EventPropertyGetterMappedSPI getGetterMappedSPI(String propertyName) {
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
