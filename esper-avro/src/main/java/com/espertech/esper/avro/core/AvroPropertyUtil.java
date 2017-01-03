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

import com.espertech.esper.avro.getter.*;
import com.espertech.esper.client.ConfigurationEventTypeAvro;
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.FragmentEventType;
import com.espertech.esper.epl.parse.ASTUtil;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.PropertySetDescriptorItem;
import com.espertech.esper.event.property.*;
import org.apache.avro.Schema;

import java.util.HashMap;
import java.util.Map;

public class AvroPropertyUtil {
    public static Class propertyType(Schema fieldSchema, Property property) {
        if (property instanceof SimpleProperty) {
            Schema.Field fieldNested = fieldSchema.getField(property.getPropertyNameAtomic());
            if (fieldNested == null) {
                return null;
            }
            return AvroTypeUtil.propertyType(fieldNested.schema());
        }

        else if (property instanceof IndexedProperty) {
            Schema.Field fieldNested = fieldSchema.getField(property.getPropertyNameAtomic());
            if (fieldNested == null || fieldNested.schema().getType() != Schema.Type.ARRAY) {
                return null;
            }
            return AvroTypeUtil.propertyType(fieldNested.schema().getElementType());
        }

        else if (property instanceof MappedProperty) {
            Schema.Field fieldNested = fieldSchema.getField(property.getPropertyNameAtomic());
            if (fieldNested == null || fieldNested.schema().getType() != Schema.Type.MAP) {
                return null;
            }
            return AvroTypeUtil.propertyType(fieldNested.schema().getValueType());
        }

        else if (property instanceof DynamicProperty) {
            Schema.Field fieldNested = fieldSchema.getField(property.getPropertyNameAtomic());
            if (fieldNested == null) {
                return Object.class;
            }
            return AvroTypeUtil.propertyType(fieldNested.schema());
        }

        NestedProperty nested = (NestedProperty) property;
        Schema current = fieldSchema;
        for (int index = 0; index < nested.getProperties().size(); index++) {
            Property levelProperty = nested.getProperties().get(index);
            if (levelProperty instanceof SimpleProperty) {
                Schema.Field fieldNested = current.getField(levelProperty.getPropertyNameAtomic());
                if (fieldNested == null) {
                    return null;
                }
                current = fieldNested.schema();
            }
            else if (levelProperty instanceof IndexedProperty) {
                Schema.Field fieldIndexed = current.getField(levelProperty.getPropertyNameAtomic());
                if (fieldIndexed == null || fieldIndexed.schema().getType() != Schema.Type.ARRAY) {
                    return null;
                }
                current = fieldIndexed.schema().getElementType();
            }
            else if (levelProperty instanceof MappedProperty){
                Schema.Field fieldMapped = current.getField(levelProperty.getPropertyNameAtomic());
                if (fieldMapped == null || fieldMapped.schema().getType() != Schema.Type.MAP) {
                    return null;
                }
                current = fieldMapped.schema().getValueType();
            }
            else if (levelProperty instanceof DynamicProperty){
                return Object.class;
            }
        }
        return AvroTypeUtil.propertyType(current);
    }

    public static FragmentEventType getFragmentType(String propertyName, Map<String, PropertySetDescriptorItem> propertyItems) {
        String unescapePropName = ASTUtil.unescapeDot(propertyName);
        PropertySetDescriptorItem item = propertyItems.get(unescapePropName);
        if (item != null) {
            return item.getFragmentEventType();
        }
        return null;
    }

    public static EventPropertyGetter getGetter(String eventTypeName, Schema avroSchema, HashMap<String, EventPropertyGetter> propertyGetterCache, Map<String, PropertySetDescriptorItem> propertyDescriptors, String propertyName, boolean addToCache, EventAdapterService eventAdapterService) {
        EventPropertyGetter getter = propertyGetterCache.get(propertyName);
        if (getter != null) {
            return getter;
        }

        String unescapePropName = ASTUtil.unescapeDot(propertyName);
        PropertySetDescriptorItem item = propertyDescriptors.get(unescapePropName);
        if (item != null) {
            getter = item.getPropertyGetter();
            mayAddToGetterCache(propertyName, propertyGetterCache, getter, true);
            return getter;
        }

        // see if this is a nested property
        int index = ASTUtil.unescapedIndexOfDot(propertyName);
        if (index == -1)
        {
            Property prop = PropertyParser.parseAndWalkLaxToSimple(propertyName);
            if (prop instanceof IndexedProperty)
            {
                IndexedProperty indexedProp = (IndexedProperty) prop;
                Schema.Field field = avroSchema.getField(prop.getPropertyNameAtomic());
                if (field == null || field.schema().getType() != Schema.Type.ARRAY) {
                    return null;
                }
                getter = new AvroEventBeanGetterIndexed(field.pos(), indexedProp.getIndex());
                mayAddToGetterCache(propertyName, propertyGetterCache, getter, addToCache);
                return getter;
            }
            else if (prop instanceof MappedProperty)
            {
                MappedProperty mappedProp = (MappedProperty) prop;
                Schema.Field field = avroSchema.getField(prop.getPropertyNameAtomic());
                if (field == null || field.schema().getType() != Schema.Type.MAP) {
                    return null;
                }
                getter = new AvroEventBeanGetterMapped(field.pos(), mappedProp.getKey());
                mayAddToGetterCache(propertyName, propertyGetterCache, getter, addToCache);
                return getter;
            }
            if (prop instanceof DynamicIndexedProperty)
            {
                DynamicIndexedProperty dynamicIndexedProp = (DynamicIndexedProperty) prop;
                getter = new AvroEventBeanGetterIndexedDynamic(prop.getPropertyNameAtomic(), dynamicIndexedProp.getIndex());
                mayAddToGetterCache(propertyName, propertyGetterCache, getter, addToCache);
                return getter;
            }
            if (prop instanceof DynamicMappedProperty)
            {
                DynamicMappedProperty dynamicMappedProp = (DynamicMappedProperty) prop;
                getter = new AvroEventBeanGetterMappedDynamic(prop.getPropertyNameAtomic(), dynamicMappedProp.getKey());
                mayAddToGetterCache(propertyName, propertyGetterCache, getter, addToCache);
                return getter;
            }
            else if (prop instanceof DynamicSimpleProperty)
            {
                getter = new AvroEventBeanGetterSimpleDynamic(prop.getPropertyNameAtomic());
                mayAddToGetterCache(propertyName, propertyGetterCache, getter, addToCache);
                return getter;
            }
            return null; // simple property already cached
        }

        // Take apart the nested property into a map key and a nested value class property name
        String propertyTop = ASTUtil.unescapeDot(propertyName.substring(0, index));
        String propertyNested = propertyName.substring(index + 1, propertyName.length());
        boolean isRootedDynamic = false;

        // If the property is dynamic, remove the ? since the property type is defined without
        if (propertyTop.endsWith("?"))
        {
            propertyTop = propertyTop.substring(0, propertyTop.length() - 1);
            isRootedDynamic = true;
        }

        Schema.Field fieldMap = avroSchema.getField(propertyTop);

        // field is known and is a record
        if (fieldMap != null && fieldMap.schema().getType() == Schema.Type.RECORD) {
            Property property = PropertyParser.parseAndWalk(propertyNested, isRootedDynamic);
            getter = AvroPropertyUtil.propertyGetterNested(eventTypeName, fieldMap.pos(), fieldMap.schema(), property, eventAdapterService);
            mayAddToGetterCache(propertyName, propertyGetterCache, getter, addToCache);
            return getter;
        }

        // field is not known or is not a record
        if (!isRootedDynamic) {
            return null;
        }
        Property property = PropertyParser.parseAndWalk(propertyNested, true);
        AvroEventPropertyGetter innerGetter = getDynamicGetter(property);
        getter = new AvroEventBeanGetterNestedDynamicPoly(propertyTop, innerGetter);
        mayAddToGetterCache(propertyName, propertyGetterCache, getter, addToCache);
        return getter;
    }

    public static FragmentEventType getFragmentEventTypeForField(String eventTypeName, Schema schema, EventAdapterService eventAdapterService) {
        if (schema.getType() != Schema.Type.RECORD) {
            return null;
        }
        String fragmentName = eventTypeName + "$" + schema.getName();
        EventType fragmentType = eventAdapterService.addAvroType(fragmentName, new ConfigurationEventTypeAvro().setAvroSchema(schema), false, false, false);
        return new FragmentEventType(fragmentType, false, false);
        // TODO support for indexed
    }

    private static EventPropertyGetter propertyGetterNested(String eventTypeName, int fieldPos, Schema fieldSchema, Property property, EventAdapterService eventAdapterService) {
        if (property instanceof SimpleProperty) {
            Schema.Field fieldNested = fieldSchema.getField(property.getPropertyNameAtomic());
            if (fieldNested == null) {
                return null;
            }
            return new AvroEventBeanGetterNestedSimple(fieldPos, fieldNested.pos());
        }

        if (property instanceof IndexedProperty) {
            IndexedProperty indexed = (IndexedProperty) property;
            Schema.Field fieldNested = fieldSchema.getField(property.getPropertyNameAtomic());
            if (fieldNested == null || fieldNested.schema().getType() != Schema.Type.ARRAY) {
                return null;
            }
            return new AvroEventBeanGetterNestedIndexed(fieldPos, fieldNested.pos(), indexed.getIndex());
        }

        if (property instanceof MappedProperty) {
            MappedProperty mapped = (MappedProperty) property;
            Schema.Field fieldNested = fieldSchema.getField(property.getPropertyNameAtomic());
            if (fieldNested == null || fieldNested.schema().getType() != Schema.Type.MAP) {
                return null;
            }
            return new AvroEventBeanGetterNestedMapped(fieldPos, fieldNested.pos(), mapped.getKey());
        }

        if (property instanceof DynamicProperty) {
            if (property instanceof DynamicSimpleProperty) {
                return new AvroEventBeanGetterNestedDynamicSimple(fieldPos, property.getPropertyNameAtomic());
            }
            throw new UnsupportedOperationException();
        }

        NestedProperty nested = (NestedProperty) property;
        boolean allSimple = true;
        for (Property levelProperty : nested.getProperties()) {
            if (!(levelProperty instanceof SimpleProperty)) {
                allSimple = false;
                break;
            }
        }
        if (allSimple) {
            Schema currentSchema = fieldSchema;
            int count = 0;
            int[] path = new int[nested.getProperties().size()];
            for (Property levelProperty : nested.getProperties()) {
                Schema.Field fieldNested = currentSchema.getField(levelProperty.getPropertyNameAtomic());
                if (fieldNested == null) {
                    return null;
                }
                currentSchema = fieldNested.schema();
                path[count] = fieldNested.pos();
                count++;
            }
            return new AvroEventBeanGetterNestedMultiLevel(fieldPos, path);
        }

        AvroEventPropertyGetter[] getters = new AvroEventPropertyGetter[nested.getProperties().size()];
        int count = 0;
        Schema currentSchema = fieldSchema;
        for (Property levelProperty : nested.getProperties()) {
            if (currentSchema == null) {
                return null;
            }

            if (levelProperty instanceof SimpleProperty) {
                Schema.Field fieldNested = currentSchema.getField(levelProperty.getPropertyNameAtomic());
                if (fieldNested == null) {
                    return null;
                }
                FragmentEventType fragmentEventType = getFragmentEventTypeForField(eventTypeName, fieldNested.schema(), eventAdapterService);
                getters[count] = new AvroEventBeanGetterSimple(fieldNested.pos(), fragmentEventType == null ? null : fragmentEventType.getFragmentType(), eventAdapterService);
                currentSchema = fieldNested.schema();
            }
            else if (levelProperty instanceof IndexedProperty) {
                IndexedProperty indexed = (IndexedProperty) levelProperty;
                Schema.Field fieldIndexed = currentSchema.getField(levelProperty.getPropertyNameAtomic());
                if (fieldIndexed == null || fieldIndexed.schema().getType() != Schema.Type.ARRAY) {
                    return null;
                }
                getters[count] = new AvroEventBeanGetterIndexed(fieldIndexed.pos(), indexed.getIndex());
                currentSchema = fieldIndexed.schema();
            }
            else if (levelProperty instanceof MappedProperty) {
                MappedProperty mapped = (MappedProperty) levelProperty;
                Schema.Field fieldMapped = currentSchema.getField(levelProperty.getPropertyNameAtomic());
                if (fieldMapped == null || fieldMapped.schema().getType() != Schema.Type.MAP) {
                    return null;
                }
                getters[count] = new AvroEventBeanGetterMapped(fieldMapped.pos(), mapped.getKey());
                currentSchema = fieldMapped.schema();
            }
            else if (levelProperty instanceof DynamicSimpleProperty) {
                Schema.Field fieldDynamic = currentSchema.getField(levelProperty.getPropertyNameAtomic());
                getters[count] = new AvroEventBeanGetterSimpleDynamic(levelProperty.getPropertyNameAtomic());
                if (fieldDynamic.schema().getType() == Schema.Type.RECORD) {
                    currentSchema = fieldDynamic.schema();
                }
                else if (fieldDynamic.schema().getType() == Schema.Type.UNION) {
                    currentSchema = AvroSchemaUtil.findUnionRecordSchemaSingle(fieldDynamic.schema());
                }
            }
            else {
                throw new UnsupportedOperationException();
            }
            count++;
        }
        return new AvroEventBeanGetterNestedPoly(fieldPos, getters);
    }

    private static AvroEventPropertyGetter getDynamicGetter(Property property) {
        if (property instanceof PropertySimple) {
            return new AvroEventBeanGetterSimpleDynamic(property.getPropertyNameAtomic());
        }
        else if (property instanceof PropertyWithIndex) {
            int index = ((PropertyWithIndex) property).getIndex();
            return new AvroEventBeanGetterIndexedDynamic(property.getPropertyNameAtomic(), index);
        }
        else if (property instanceof PropertyWithKey) {
            String key = ((PropertyWithKey) property).getKey();
            return new AvroEventBeanGetterMappedDynamic(property.getPropertyNameAtomic(), key);
        }

        NestedProperty nested = (NestedProperty) property;
        AvroEventPropertyGetter[] getters = new AvroEventPropertyGetter[nested.getProperties().size()];
        int count = 0;
        for (Property levelProperty : nested.getProperties()) {
            getters[count] = getDynamicGetter(levelProperty);
            count++;
        }
        return new AvroEventBeanGetterDynamicPoly(getters);
    }

    private static void mayAddToGetterCache(String propertyName, HashMap<String, EventPropertyGetter> propertyGetterCache, EventPropertyGetter getter, boolean add) {
        if (!add) {
            return;
        }
        propertyGetterCache.put(propertyName, getter);
    }
}
