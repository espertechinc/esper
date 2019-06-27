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
package com.espertech.esper.common.internal.event.json.core;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventPropertyDescriptor;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.json.minimaljson.JsonParser;
import com.espertech.esper.common.client.meta.EventTypeMetadata;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactory;
import com.espertech.esper.common.internal.event.core.*;
import com.espertech.esper.common.internal.event.json.compiletime.JsonUnderlyingField;
import com.espertech.esper.common.internal.event.json.parser.core.JsonDelegateBase;
import com.espertech.esper.common.internal.event.json.parser.core.JsonDelegateFactory;
import com.espertech.esper.common.internal.event.json.parser.core.JsonHandlerDelegator;
import com.espertech.esper.common.internal.event.json.writer.*;
import com.espertech.esper.common.internal.event.property.IndexedProperty;
import com.espertech.esper.common.internal.event.property.MappedProperty;
import com.espertech.esper.common.internal.event.property.Property;
import com.espertech.esper.common.internal.event.property.PropertyParser;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.*;

/**
 * Implementation of the EventType interface for handling JavaBean-type classes.
 */
public class JsonEventType extends BaseNestableEventType {
    private final JsonEventTypeDetail detail;

    private Class delegateType;
    private JsonDelegateFactory delegateFactory;
    private Class underlyingType;
    protected EventPropertyDescriptor[] writablePropertyDescriptors;
    protected Map<String, Pair<EventPropertyDescriptor, JsonEventBeanPropertyWriter>> propertyWriters;

    public JsonEventType(EventTypeMetadata metadata, Map<String, Object> propertyTypes, EventType[] optionalSuperTypes, Set<EventType> optionalDeepSupertypes, String startTimestampPropertyName, String endTimestampPropertyName, EventTypeNestableGetterFactory getterFactory, BeanEventTypeFactory beanEventTypeFactory, JsonEventTypeDetail detail, Class underlyingStandInClass) {
        super(metadata, propertyTypes, optionalSuperTypes, optionalDeepSupertypes, startTimestampPropertyName, endTimestampPropertyName, getterFactory, beanEventTypeFactory, true);
        this.detail = detail;
        this.underlyingType = underlyingStandInClass;
    }

    public JsonEventBeanPropertyWriter getWriter(String propertyName) {
        if (writablePropertyDescriptors == null) {
            initializeWriters();
        }
        Pair<EventPropertyDescriptor, JsonEventBeanPropertyWriter> pair = propertyWriters.get(propertyName);
        if (pair != null) {
            return pair.getSecond();
        }

        Property property = PropertyParser.parseAndWalkLaxToSimple(propertyName);
        if (property instanceof MappedProperty) {
            MappedProperty mapProp = (MappedProperty) property;
            JsonUnderlyingField field = detail.getFieldDescriptors().get(mapProp.getPropertyNameAtomic());
            if (field == null) {
                return null;
            }
            return new JsonEventBeanPropertyWriterMapProp(this.delegateFactory, field, mapProp.getKey());
        }

        if (property instanceof IndexedProperty) {
            IndexedProperty indexedProp = (IndexedProperty) property;
            JsonUnderlyingField field = detail.getFieldDescriptors().get(indexedProp.getPropertyNameAtomic());
            if (field == null) {
                return null;
            }
            return new JsonEventBeanPropertyWriterIndexedProp(this.delegateFactory, field, indexedProp.getIndex());
        }

        return null;
    }

    public EventPropertyDescriptor[] getWriteableProperties() {
        if (writablePropertyDescriptors == null) {
            initializeWriters();
        }
        return writablePropertyDescriptors;
    }

    public EventPropertyDescriptor getWritableProperty(String propertyName) {
        if (writablePropertyDescriptors == null) {
            initializeWriters();
        }
        Pair<EventPropertyDescriptor, ? extends EventPropertyWriter> pair = propertyWriters.get(propertyName);
        if (pair != null) {
            return pair.getFirst();
        }

        Property property = PropertyParser.parseAndWalkLaxToSimple(propertyName);
        if (property instanceof MappedProperty) {
            EventPropertyWriter writer = getWriter(propertyName);
            if (writer == null) {
                return null;
            }
            MappedProperty mapProp = (MappedProperty) property;
            return new EventPropertyDescriptor(mapProp.getPropertyNameAtomic(), Object.class, null, false, true, false, true, false);
        }
        if (property instanceof IndexedProperty) {
            EventPropertyWriter writer = getWriter(propertyName);
            if (writer == null) {
                return null;
            }
            IndexedProperty indexedProp = (IndexedProperty) property;
            return new EventPropertyDescriptor(indexedProp.getPropertyNameAtomic(), Object.class, null, true, false, true, false, false);
        }
        return null;
    }

    public EventBeanCopyMethodForge getCopyMethodForge(String[] properties) {
        return new JsonEventBeanCopyMethodForge(this);
    }

    public EventBeanWriter getWriter(String[] properties) {
        if (writablePropertyDescriptors == null) {
            initializeWriters();
        }

        JsonEventBeanPropertyWriter[] writers = new JsonEventBeanPropertyWriter[properties.length];
        for (int i = 0; i < properties.length; i++) {
            writers[i] = getWriter(properties[i]);
            if (writers[i] == null) {
                return null;
            }
        }
        return new JsonEventBeanWriterPerProp(writers);
    }

    public Class getUnderlyingType() {
        if (underlyingType == null) {
            throw new EPException("Underlying type has not been set");
        }
        return underlyingType;
    }

    public void initialize(ClassLoader classLoader) {
        // resolve underlying type
        try {
            underlyingType = Class.forName(detail.getUnderlyingClassName(), true, classLoader);
        } catch (ClassNotFoundException ex) {
            throw new EPException("Failed to load Json underlying class: " + ex.getMessage(), ex);
        }

        // resolve delegate
        try {
            this.delegateType = Class.forName(detail.getDelegateClassName(), true, classLoader);
        } catch (ClassNotFoundException e) {
            throw new EPException("Failed to find class: " + e.getMessage(), e);
        }

        // resolve handler factory
        Class delegateFactory;
        try {
            delegateFactory = Class.forName(detail.getDelegateFactoryClassName(), true, classLoader);
        } catch (ClassNotFoundException e) {
            throw new EPException("Failed to find class: " + e.getMessage(), e);
        }
        this.delegateFactory = (JsonDelegateFactory) JavaClassHelper.instantiate(JsonDelegateFactory.class, delegateFactory);
    }

    public Object parse(String json) {
        try {
            JsonHandlerDelegator handler = new JsonHandlerDelegator();
            JsonDelegateBase delegate = delegateFactory.make(handler, null);
            handler.setDelegate(delegate);
            JsonParser parser = new JsonParser(handler);
            parser.parse(json);
            return delegate.getResult();
        } catch (EPException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new EPException("Failed to parse Json: " + ex.getMessage(), ex);
        }
    }

    public JsonEventTypeDetail getDetail() {
        return detail;
    }

    public Class getDelegateType() {
        return delegateType;
    }

    public JsonDelegateFactory getDelegateFactory() {
        return delegateFactory;
    }

    public int getColumnNumber(String columnName) {
        JsonUnderlyingField field = detail.getFieldDescriptors().get(columnName);
        if (field != null) {
            return field.getPropertyNumber();
        }
        throw new IllegalStateException("Unrecognized json-type column name '" + columnName + "'");
    }

    public boolean isDeepEqualsConsiderOrder(JsonEventType other) {
        if (other.nestableTypes.size() != nestableTypes.size()) {
            return false;
        }

        for (Map.Entry<String, Object> propMeEntry : nestableTypes.entrySet()) {
            JsonUnderlyingField fieldMe = detail.getFieldDescriptors().get(propMeEntry.getKey());
            JsonUnderlyingField fieldOther = other.detail.getFieldDescriptors().get(propMeEntry.getKey());
            if (fieldOther == null || fieldMe.getPropertyNumber() != fieldOther.getPropertyNumber()) {
                return false;
            }

            String propName = propMeEntry.getKey();
            Object setOneType = this.nestableTypes.get(propName);
            Object setTwoType = other.nestableTypes.get(propName);
            boolean setTwoTypeFound = other.nestableTypes.containsKey(propName);

            ExprValidationException comparedMessage = BaseNestableEventUtil.comparePropType(propName, setOneType, setTwoType, setTwoTypeFound, other.getName());
            if (comparedMessage != null) {
                return false;
            }
        }

        return true;
    }

    private void initializeWriters() {
        List<EventPropertyDescriptor> writeableProps = new ArrayList<EventPropertyDescriptor>();
        Map<String, Pair<EventPropertyDescriptor, JsonEventBeanPropertyWriter>> propertWritersMap = new HashMap<String, Pair<EventPropertyDescriptor, JsonEventBeanPropertyWriter>>();
        for (EventPropertyDescriptor prop : propertyDescriptors) {
            JsonUnderlyingField field = detail.getFieldDescriptors().get(prop.getPropertyName());
            if (field == null) {
                continue;
            }
            writeableProps.add(prop);
            JsonEventBeanPropertyWriter eventPropertyWriter = new JsonEventBeanPropertyWriter(this.delegateFactory, field);
            propertWritersMap.put(prop.getPropertyName(), new Pair<>(prop, eventPropertyWriter));
        }

        propertyWriters = propertWritersMap;
        writablePropertyDescriptors = writeableProps.toArray(new EventPropertyDescriptor[writeableProps.size()]);
    }
}
