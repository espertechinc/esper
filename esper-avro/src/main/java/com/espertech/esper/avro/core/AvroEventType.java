/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.avro.core;

import com.espertech.esper.avro.getter.AvroEventBeanGetterSimple;
import com.espertech.esper.client.*;
import com.espertech.esper.epl.parse.ASTUtil;
import com.espertech.esper.event.*;
import com.espertech.esper.event.avro.AvroMarkerEventType;
import com.espertech.esper.event.property.*;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class AvroEventType implements AvroMarkerEventType, EventTypeSPI {
    private final EventTypeMetadata metadata;
    private final String eventTypeName;
    private final int typeId;
    private final EventAdapterService eventAdapterService;
    private final Schema avroSchema;
    private final Map<String, PropertySetDescriptorItem> propertyItems;

    private EventPropertyDescriptor[] propertyDescriptors;
    private String[] propertyNames;
    private HashMap<String, EventPropertyGetter> propertyGetterCache;

    public AvroEventType(EventTypeMetadata metadata, String eventTypeName, int typeId, EventAdapterService eventAdapterService, Schema avroSchema) {
        this.metadata = metadata;
        this.eventTypeName = eventTypeName;
        this.typeId = typeId;
        this.eventAdapterService = eventAdapterService;
        this.avroSchema = avroSchema;

        propertyItems = new LinkedHashMap<>();
        init();
    }

    public Class getUnderlyingType() {
        return GenericData.Record.class;
    }

    public Class getPropertyType(String propertyName) {
        PropertySetDescriptorItem item = propertyItems.get(ASTUtil.unescapeDot(propertyName));
        if (item != null) {
            return item.getSimplePropertyType();
        }

        Property property = PropertyParser.parseAndWalkLaxToSimple(propertyName);
        return AvroPropertyUtil.propertyType(avroSchema, property);
    }

    public boolean isProperty(String propertyExpression) {
        Class propertyType = getPropertyType(propertyExpression);
        if (propertyType != null) {
            return true;
        }
        if (propertyGetterCache == null) {
            propertyGetterCache = new HashMap<>();
        }
        return AvroPropertyUtil.getGetter(eventTypeName, avroSchema, propertyGetterCache, propertyItems, propertyExpression, false, eventAdapterService) != null;
    }

    public EventPropertyGetter getGetter(String propertyExpression) {
        if (propertyGetterCache == null) {
            propertyGetterCache = new HashMap<>();
        }
        return AvroPropertyUtil.getGetter(eventTypeName, avroSchema, propertyGetterCache, propertyItems, propertyExpression, true, eventAdapterService);
    }

    public FragmentEventType getFragmentType(String propertyExpression) {
        return AvroPropertyUtil.getFragmentType(propertyExpression, propertyItems);
    }

    public String[] getPropertyNames() {
        return propertyNames;
    }

    public EventPropertyDescriptor[] getPropertyDescriptors() {
        return propertyDescriptors;
    }

    public EventPropertyDescriptor getPropertyDescriptor(String propertyName) {
        PropertySetDescriptorItem item = propertyItems.get(propertyName);
        if (item == null) {
            return null;
        }
        return item.getPropertyDescriptor();
    }

    public EventType[] getSuperTypes() {
        return null;
    }

    public Iterator<EventType> getDeepSuperTypes() {
        // TODO
        throw new UnsupportedOperationException();
    }

    public String getName() {
        return metadata.getPublicName();
    }

    public EventPropertyGetterMapped getGetterMapped(String mappedPropertyName) {
        // TODO
        throw new UnsupportedOperationException();
    }

    public EventPropertyGetterIndexed getGetterIndexed(String indexedPropertyName) {
        // TODO
        throw new UnsupportedOperationException();
    }

    public int getEventTypeId() {
        // TODO
        throw new UnsupportedOperationException();
    }

    public String getStartTimestampPropertyName() {
        // TODO
        throw new UnsupportedOperationException();
    }

    public String getEndTimestampPropertyName() {
        // TODO
        throw new UnsupportedOperationException();
    }

    private void init() {
        propertyNames = new String[avroSchema.getFields().size()];
        propertyDescriptors = new EventPropertyDescriptor[propertyNames.length];
        int fieldNum = 0;

        for (Schema.Field field : avroSchema.getFields()) {
            propertyNames[fieldNum] = field.name();

            Class propertyType = AvroTypeUtil.propertyType(field.schema());
            Class componentType = null;
            boolean indexed = false;
            boolean mapped = false;
            FragmentEventType fragmentEventType = AvroPropertyUtil.getFragmentEventTypeForField(eventTypeName, field.schema(), eventAdapterService);

            if (field.schema().getType() == Schema.Type.ARRAY) {
                componentType = AvroTypeUtil.propertyType(field.schema().getElementType());
                indexed = true;
                if (field.schema().getElementType().getType() == Schema.Type.RECORD) {
                    // TODO fragment = true;
                }
            }
            else if (field.schema().getType() == Schema.Type.MAP) {
                mapped = true;
            }
            AvroEventBeanGetterSimple getter = new AvroEventBeanGetterSimple(field.pos(), fragmentEventType == null ? null : fragmentEventType.getFragmentType(), eventAdapterService);

            EventPropertyDescriptor descriptor = new EventPropertyDescriptor(field.name(), propertyType, componentType, false, false, indexed, mapped, fragmentEventType != null);
            PropertySetDescriptorItem item = new PropertySetDescriptorItem(descriptor, propertyType, getter, fragmentEventType);
            propertyItems.put(field.name(), item);
            propertyDescriptors[fieldNum] = descriptor;

            fieldNum++;
        }
    }

    public EventTypeMetadata getMetadata() {
        return metadata;
    }

    public EventPropertyWriter getWriter(String propertyName) {
        // TODO
        throw new UnsupportedOperationException();
    }

    public EventPropertyDescriptor[] getWriteableProperties() {
        // TODO
        throw new UnsupportedOperationException();
    }

    public EventPropertyDescriptor getWritableProperty(String propertyName) {
        // TODO
        throw new UnsupportedOperationException();
    }

    public EventBeanCopyMethod getCopyMethod(String[] properties) {
        // TODO
        throw new UnsupportedOperationException();
    }

    public EventBeanWriter getWriter(String[] properties) {
        // TODO
        throw new UnsupportedOperationException();
    }

    public EventBeanReader getReader() {
        // TODO
        throw new UnsupportedOperationException();
    }

    public boolean equalsCompareType(EventType other) {
        if (!(other instanceof AvroEventType)) {
            return false;
        }
        AvroEventType otherAvro = (AvroEventType) other;
        if (!otherAvro.getName().equals(metadata.getPrimaryName())) {
            return false;
        }
        return otherAvro.avroSchema.equals(avroSchema);
    }

    public Schema getSchema() {
        return avroSchema;
    }
}
