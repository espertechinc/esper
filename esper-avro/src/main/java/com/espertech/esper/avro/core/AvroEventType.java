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
package com.espertech.esper.avro.core;

import com.espertech.esper.avro.getter.AvroEventBeanGetterIndexedRuntimeKeyed;
import com.espertech.esper.avro.getter.AvroEventBeanGetterMappedRuntimeKeyed;
import com.espertech.esper.avro.getter.AvroEventBeanGetterSimple;
import com.espertech.esper.avro.writer.*;
import com.espertech.esper.client.*;
import com.espertech.esper.epl.parse.ASTUtil;
import com.espertech.esper.event.*;
import com.espertech.esper.event.avro.AvroSchemaEventType;
import com.espertech.esper.event.property.IndexedProperty;
import com.espertech.esper.event.property.MappedProperty;
import com.espertech.esper.event.property.Property;
import com.espertech.esper.event.property.PropertyParser;
import com.espertech.esper.util.CollectionUtil;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import java.util.*;

import static com.espertech.esper.avro.core.AvroFragmentTypeUtil.getFragmentEventTypeForField;

public class AvroEventType implements AvroSchemaEventType, EventTypeSPI {
    private final EventTypeMetadata metadata;
    private final int typeId;
    private final EventAdapterService eventAdapterService;
    private final Schema avroSchema;
    private final Map<String, PropertySetDescriptorItem> propertyItems;
    private final String startTimestampPropertyName;
    private final String endTimestampPropertyName;
    private final EventType[] optionalSuperTypes;
    private final Set<EventType> deepSupertypes;

    private EventPropertyDescriptor[] propertyDescriptors;
    private String[] propertyNames;
    private HashMap<String, EventPropertyGetterSPI> propertyGetterCache;
    private Map<String, EventPropertyGetter> propertyGetterCodegeneratedCache;

    public AvroEventType(EventTypeMetadata metadata,
                         String eventTypeName,
                         int typeId,
                         EventAdapterService eventAdapterService,
                         Schema avroSchema,
                         String startTimestampPropertyName,
                         String endTimestampPropertyName,
                         EventType[] optionalSuperTypes,
                         Set<EventType> deepSupertypes) {
        this.metadata = metadata;
        this.typeId = typeId;
        this.eventAdapterService = eventAdapterService;
        this.avroSchema = avroSchema;
        this.optionalSuperTypes = optionalSuperTypes;
        this.deepSupertypes = deepSupertypes == null ? Collections.emptySet() : deepSupertypes;
        this.propertyItems = new LinkedHashMap<>();

        init();

        EventTypeUtility.TimestampPropertyDesc desc = EventTypeUtility.validatedDetermineTimestampProps(this, startTimestampPropertyName, endTimestampPropertyName, optionalSuperTypes);
        this.startTimestampPropertyName = desc.getStart();
        this.endTimestampPropertyName = desc.getEnd();
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
        return AvroPropertyUtil.getGetter(avroSchema, propertyGetterCache, propertyItems, propertyExpression, false, eventAdapterService) != null;
    }

    public EventPropertyGetterSPI getGetterSPI(String propertyExpression) {
        if (propertyGetterCache == null) {
            propertyGetterCache = new HashMap<>();
        }
        return AvroPropertyUtil.getGetter(avroSchema, propertyGetterCache, propertyItems, propertyExpression, true, eventAdapterService);
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

    public FragmentEventType getFragmentType(String propertyExpression) {
        return AvroFragmentTypeUtil.getFragmentType(avroSchema, propertyExpression, propertyItems, eventAdapterService);
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
        return optionalSuperTypes;
    }

    public Iterator<EventType> getDeepSuperTypes() {
        return deepSupertypes.iterator();
    }

    public String getName() {
        return metadata.getPublicName();
    }

    public EventPropertyGetterMapped getGetterMapped(String mappedPropertyName) {
        EventPropertyGetterMappedSPI getter = getGetterMappedSPI(mappedPropertyName);
        if (getter == null) {
            return null;
        }
        if (!eventAdapterService.getEngineImportService().isCodegenEventPropertyGetters()) {
            return getter;
        }
        return eventAdapterService.getEngineImportService().codegenGetter(getter, metadata.getPublicName(), mappedPropertyName);
    }

    public EventPropertyGetterMappedSPI getGetterMappedSPI(String mappedPropertyName) {
        PropertySetDescriptorItem desc = propertyItems.get(mappedPropertyName);
        if (desc == null || !desc.getPropertyDescriptor().isMapped()) {
            return null;
        }
        Schema.Field field = avroSchema.getField(mappedPropertyName);
        return new AvroEventBeanGetterMappedRuntimeKeyed(field.pos());
    }

    public EventPropertyGetterIndexed getGetterIndexed(String indexedPropertyName) {
        EventPropertyGetterIndexedSPI getter = getGetterIndexedSPI(indexedPropertyName);
        if (getter == null) {
            return null;
        }
        if (!eventAdapterService.getEngineImportService().isCodegenEventPropertyGetters()) {
            return getter;
        }
        return eventAdapterService.getEngineImportService().codegenGetter(getter, metadata.getPublicName(), indexedPropertyName);
    }

    public EventPropertyGetterIndexedSPI getGetterIndexedSPI(String indexedPropertyName) {
        PropertySetDescriptorItem desc = propertyItems.get(indexedPropertyName);
        if (desc == null || !desc.getPropertyDescriptor().isIndexed()) {
            return null;
        }
        Schema.Field field = avroSchema.getField(indexedPropertyName);
        return new AvroEventBeanGetterIndexedRuntimeKeyed(field.pos());
    }

    public int getEventTypeId() {
        return typeId;
    }

    public String getStartTimestampPropertyName() {
        return startTimestampPropertyName;
    }

    public String getEndTimestampPropertyName() {
        return endTimestampPropertyName;
    }

    public EventTypeMetadata getMetadata() {
        return metadata;
    }

    public AvroEventBeanPropertyWriter getWriter(String propertyName) {
        PropertySetDescriptorItem desc = propertyItems.get(propertyName);
        if (desc != null) {
            int pos = avroSchema.getField(propertyName).pos();
            return new AvroEventBeanPropertyWriter(pos);
        }

        Property property = PropertyParser.parseAndWalkLaxToSimple(propertyName);
        if (property instanceof MappedProperty) {
            MappedProperty mapProp = (MappedProperty) property;
            int pos = avroSchema.getField(property.getPropertyNameAtomic()).pos();
            return new AvroEventBeanPropertyWriterMapProp(pos, mapProp.getKey());
        }

        if (property instanceof IndexedProperty) {
            IndexedProperty indexedProp = (IndexedProperty) property;
            int pos = avroSchema.getField(property.getPropertyNameAtomic()).pos();
            return new AvroEventBeanPropertyWriterIndexedProp(pos, indexedProp.getIndex());
        }

        return null;
    }

    public EventPropertyDescriptor[] getWriteableProperties() {
        return propertyDescriptors;
    }

    public EventPropertyDescriptor getWritableProperty(String propertyName) {
        for (EventPropertyDescriptor desc : propertyDescriptors) {
            if (desc.getPropertyName().equals(propertyName)) {
                return desc;
            }
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

    public EventBeanCopyMethod getCopyMethod(String[] properties) {
        return new AvroEventBeanCopyMethod(this, eventAdapterService);
    }

    public EventBeanWriter getWriter(String[] properties) {
        boolean allSimpleProps = true;
        AvroEventBeanPropertyWriter[] writers = new AvroEventBeanPropertyWriter[properties.length];
        List<Integer> indexes = new ArrayList<Integer>();

        for (int i = 0; i < properties.length; i++) {
            AvroEventBeanPropertyWriter writer = getWriter(properties[i]);
            if (propertyItems.containsKey(properties[i])) {
                writers[i] = writer;
                indexes.add(avroSchema.getField(properties[i]).pos());
            } else {
                writers[i] = getWriter(properties[i]);
                if (writers[i] == null) {
                    return null;
                }
                allSimpleProps = false;
            }
        }

        if (allSimpleProps) {
            return new AvroEventBeanWriterSimpleProps(CollectionUtil.intArray(indexes));
        }
        return new AvroEventBeanWriterPerProp(writers);
    }

    public EventBeanReader getReader() {
        return null; // use the default reader
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

    public Object getSchema() {
        return avroSchema;
    }

    public Schema getSchemaAvro() {
        return avroSchema;
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
            FragmentEventType fragmentEventType = null;

            if (field.schema().getType() == Schema.Type.ARRAY) {
                componentType = AvroTypeUtil.propertyType(field.schema().getElementType());
                indexed = true;
                if (field.schema().getElementType().getType() == Schema.Type.RECORD) {
                    fragmentEventType = getFragmentEventTypeForField(field.schema(), eventAdapterService);
                }
            } else if (field.schema().getType() == Schema.Type.MAP) {
                mapped = true;
                componentType = AvroTypeUtil.propertyType(field.schema().getValueType());
            } else {
                fragmentEventType = getFragmentEventTypeForField(field.schema(), eventAdapterService);
            }
            AvroEventBeanGetterSimple getter = new AvroEventBeanGetterSimple(field.pos(), fragmentEventType == null ? null : fragmentEventType.getFragmentType(), eventAdapterService, propertyType);

            EventPropertyDescriptor descriptor = new EventPropertyDescriptor(field.name(), propertyType, componentType, false, false, indexed, mapped, fragmentEventType != null);
            PropertySetDescriptorItem item = new PropertySetDescriptorItem(descriptor, propertyType, getter, fragmentEventType);
            propertyItems.put(field.name(), item);
            propertyDescriptors[fieldNum] = descriptor;

            fieldNum++;
        }
    }
}
