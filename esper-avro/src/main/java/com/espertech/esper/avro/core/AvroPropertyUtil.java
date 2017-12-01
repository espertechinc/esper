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

import com.espertech.esper.avro.getter.*;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.FragmentEventType;
import com.espertech.esper.epl.parse.ASTUtil;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.EventPropertyGetterSPI;
import com.espertech.esper.event.PropertySetDescriptorItem;
import com.espertech.esper.event.property.*;
import com.espertech.esper.util.StringValue;
import org.apache.avro.Schema;

import java.util.HashMap;
import java.util.Map;

import static com.espertech.esper.avro.core.AvroFragmentTypeUtil.getFragmentEventTypeForField;

public class AvroPropertyUtil {
    protected static Class propertyType(Schema fieldSchema, Property property) {
        AvroFieldDescriptor desc = AvroFieldUtil.fieldForProperty(fieldSchema, property);
        if (desc == null) {
            return null;
        }
        if (desc.isDynamic()) {
            return Object.class;
        }
        Schema typeSchema = desc.getField().schema();
        if (desc.isAccessedByIndex()) {
            typeSchema = desc.getField().schema().getElementType();
        } else if (desc.isAccessedByKey()) {
            typeSchema = desc.getField().schema().getValueType();
        }
        return AvroTypeUtil.propertyType(typeSchema);
    }

    protected static EventPropertyGetterSPI getGetter(Schema avroSchema, HashMap<String, EventPropertyGetterSPI> propertyGetterCache, Map<String, PropertySetDescriptorItem> propertyDescriptors, String propertyName, boolean addToCache, EventAdapterService eventAdapterService) {
        EventPropertyGetterSPI getter = propertyGetterCache.get(propertyName);
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
        int index = StringValue.unescapedIndexOfDot(propertyName);
        if (index == -1) {
            Property prop = PropertyParser.parseAndWalkLaxToSimple(propertyName);
            if (prop instanceof IndexedProperty) {
                IndexedProperty indexedProp = (IndexedProperty) prop;
                Schema.Field field = avroSchema.getField(prop.getPropertyNameAtomic());
                if (field == null || field.schema().getType() != Schema.Type.ARRAY) {
                    return null;
                }
                FragmentEventType fragmentEventType = AvroFragmentTypeUtil.getFragmentEventTypeForField(field.schema(), eventAdapterService);
                getter = new AvroEventBeanGetterIndexed(field.pos(), indexedProp.getIndex(), fragmentEventType == null ? null : fragmentEventType.getFragmentType(), eventAdapterService);
                mayAddToGetterCache(propertyName, propertyGetterCache, getter, addToCache);
                return getter;
            } else if (prop instanceof MappedProperty) {
                MappedProperty mappedProp = (MappedProperty) prop;
                Schema.Field field = avroSchema.getField(prop.getPropertyNameAtomic());
                if (field == null || field.schema().getType() != Schema.Type.MAP) {
                    return null;
                }
                getter = new AvroEventBeanGetterMapped(field.pos(), mappedProp.getKey());
                mayAddToGetterCache(propertyName, propertyGetterCache, getter, addToCache);
                return getter;
            }
            if (prop instanceof DynamicIndexedProperty) {
                DynamicIndexedProperty dynamicIndexedProp = (DynamicIndexedProperty) prop;
                getter = new AvroEventBeanGetterIndexedDynamic(prop.getPropertyNameAtomic(), dynamicIndexedProp.getIndex());
                mayAddToGetterCache(propertyName, propertyGetterCache, getter, addToCache);
                return getter;
            }
            if (prop instanceof DynamicMappedProperty) {
                DynamicMappedProperty dynamicMappedProp = (DynamicMappedProperty) prop;
                getter = new AvroEventBeanGetterMappedDynamic(prop.getPropertyNameAtomic(), dynamicMappedProp.getKey());
                mayAddToGetterCache(propertyName, propertyGetterCache, getter, addToCache);
                return getter;
            } else if (prop instanceof DynamicSimpleProperty) {
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
        if (propertyTop.endsWith("?")) {
            propertyTop = propertyTop.substring(0, propertyTop.length() - 1);
            isRootedDynamic = true;
        }

        Property propTop = PropertyParser.parseAndWalkLaxToSimple(propertyTop);
        Schema.Field fieldTop = avroSchema.getField(propTop.getPropertyNameAtomic());

        // field is known and is a record
        if (fieldTop != null && fieldTop.schema().getType() == Schema.Type.RECORD && propTop instanceof SimpleProperty) {
            GetterNestedFactoryRootedSimple factory = new GetterNestedFactoryRootedSimple(eventAdapterService, fieldTop.pos());
            Property property = PropertyParser.parseAndWalk(propertyNested, isRootedDynamic);
            getter = propertyGetterNested(factory, fieldTop.schema(), property, eventAdapterService);
            mayAddToGetterCache(propertyName, propertyGetterCache, getter, addToCache);
            return getter;
        }

        // field is known and is a record
        if (fieldTop != null && fieldTop.schema().getType() == Schema.Type.ARRAY && propTop instanceof IndexedProperty) {
            GetterNestedFactoryRootedIndexed factory = new GetterNestedFactoryRootedIndexed(eventAdapterService, fieldTop.pos(), ((IndexedProperty) propTop).getIndex());
            Property property = PropertyParser.parseAndWalk(propertyNested, isRootedDynamic);
            getter = propertyGetterNested(factory, fieldTop.schema().getElementType(), property, eventAdapterService);
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

    private static EventPropertyGetterSPI propertyGetterNested(GetterNestedFactory factory, Schema fieldSchema, Property property, EventAdapterService eventAdapterService) {
        if (property instanceof SimpleProperty) {
            Schema.Field fieldNested = fieldSchema.getField(property.getPropertyNameAtomic());
            if (fieldNested == null) {
                return null;
            }
            FragmentEventType fragmentEventType = AvroFragmentTypeUtil.getFragmentEventTypeForField(fieldNested.schema(), eventAdapterService);
            return factory.makeSimple(fieldNested.pos(), fragmentEventType == null ? null : fragmentEventType.getFragmentType(), AvroTypeUtil.propertyType(fieldNested.schema()));
        }

        if (property instanceof IndexedProperty) {
            IndexedProperty indexed = (IndexedProperty) property;
            Schema.Field fieldNested = fieldSchema.getField(property.getPropertyNameAtomic());
            if (fieldNested == null || fieldNested.schema().getType() != Schema.Type.ARRAY) {
                return null;
            }
            FragmentEventType fragmentEventType = AvroFragmentTypeUtil.getFragmentEventTypeForField(fieldNested.schema(), eventAdapterService);
            return factory.makeIndexed(fieldNested.pos(), indexed.getIndex(), fragmentEventType == null ? null : fragmentEventType.getFragmentType());
        }

        if (property instanceof MappedProperty) {
            MappedProperty mapped = (MappedProperty) property;
            Schema.Field fieldNested = fieldSchema.getField(property.getPropertyNameAtomic());
            if (fieldNested == null || fieldNested.schema().getType() != Schema.Type.MAP) {
                return null;
            }
            return factory.makeMapped(fieldNested.pos(), mapped.getKey());
        }

        if (property instanceof DynamicProperty) {
            if (property instanceof DynamicSimpleProperty) {
                return factory.makeDynamicSimple(property.getPropertyNameAtomic());
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
            Class[] types = new Class[nested.getProperties().size()];
            for (Property levelProperty : nested.getProperties()) {
                if (currentSchema.getType() != Schema.Type.RECORD) {
                    return null;
                }
                Schema.Field fieldNested = currentSchema.getField(levelProperty.getPropertyNameAtomic());
                if (fieldNested == null) {
                    return null;
                }
                currentSchema = fieldNested.schema();
                path[count] = fieldNested.pos();
                types[count] = AvroTypeUtil.propertyType(currentSchema);
                count++;
            }
            FragmentEventType fragmentEventType = AvroFragmentTypeUtil.getFragmentEventTypeForField(currentSchema, eventAdapterService);
            return factory.makeNestedSimpleMultiLevel(path, types, fragmentEventType == null ? null : fragmentEventType.getFragmentType());
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
                FragmentEventType fragmentEventType = getFragmentEventTypeForField(fieldNested.schema(), eventAdapterService);
                Class propertyType = AvroTypeUtil.propertyType(fieldNested.schema());
                getters[count] = new AvroEventBeanGetterSimple(fieldNested.pos(), fragmentEventType == null ? null : fragmentEventType.getFragmentType(), eventAdapterService, propertyType);
                currentSchema = fieldNested.schema();
            } else if (levelProperty instanceof IndexedProperty) {
                IndexedProperty indexed = (IndexedProperty) levelProperty;
                Schema.Field fieldIndexed = currentSchema.getField(levelProperty.getPropertyNameAtomic());
                if (fieldIndexed == null || fieldIndexed.schema().getType() != Schema.Type.ARRAY) {
                    return null;
                }
                FragmentEventType fragmentEventType = AvroFragmentTypeUtil.getFragmentEventTypeForField(fieldIndexed.schema(), eventAdapterService);
                getters[count] = new AvroEventBeanGetterIndexed(fieldIndexed.pos(), indexed.getIndex(), fragmentEventType == null ? null : fragmentEventType.getFragmentType(), eventAdapterService);
                currentSchema = fieldIndexed.schema().getElementType();
            } else if (levelProperty instanceof MappedProperty) {
                MappedProperty mapped = (MappedProperty) levelProperty;
                Schema.Field fieldMapped = currentSchema.getField(levelProperty.getPropertyNameAtomic());
                if (fieldMapped == null || fieldMapped.schema().getType() != Schema.Type.MAP) {
                    return null;
                }
                getters[count] = new AvroEventBeanGetterMapped(fieldMapped.pos(), mapped.getKey());
                currentSchema = fieldMapped.schema();
            } else if (levelProperty instanceof DynamicSimpleProperty) {
                if (currentSchema.getType() != Schema.Type.RECORD) {
                    return null;
                }
                Schema.Field fieldDynamic = currentSchema.getField(levelProperty.getPropertyNameAtomic());
                getters[count] = new AvroEventBeanGetterSimpleDynamic(levelProperty.getPropertyNameAtomic());
                if (fieldDynamic.schema().getType() == Schema.Type.RECORD) {
                    currentSchema = fieldDynamic.schema();
                } else if (fieldDynamic.schema().getType() == Schema.Type.UNION) {
                    currentSchema = AvroSchemaUtil.findUnionRecordSchemaSingle(fieldDynamic.schema());
                }
            } else {
                throw new UnsupportedOperationException();
            }
            count++;
        }
        return factory.makeNestedPolyMultiLevel(getters);
    }

    private static AvroEventPropertyGetter getDynamicGetter(Property property) {
        if (property instanceof PropertySimple) {
            return new AvroEventBeanGetterSimpleDynamic(property.getPropertyNameAtomic());
        } else if (property instanceof PropertyWithIndex) {
            int index = ((PropertyWithIndex) property).getIndex();
            return new AvroEventBeanGetterIndexedDynamic(property.getPropertyNameAtomic(), index);
        } else if (property instanceof PropertyWithKey) {
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

    private static void mayAddToGetterCache(String propertyName, HashMap<String, EventPropertyGetterSPI> propertyGetterCache, EventPropertyGetterSPI getter, boolean add) {
        if (!add) {
            return;
        }
        propertyGetterCache.put(propertyName, getter);
    }

    private interface GetterNestedFactory {
        EventPropertyGetterSPI makeSimple(int posNested, EventType fragmentEventType, Class propertyType);

        EventPropertyGetterSPI makeIndexed(int posNested, int index, EventType fragmentEventType);

        EventPropertyGetterSPI makeMapped(int posNested, String key);

        EventPropertyGetterSPI makeDynamicSimple(String propertyName);

        EventPropertyGetterSPI makeNestedSimpleMultiLevel(int[] path, Class[] propertyTypes, EventType fragmentEventType);

        EventPropertyGetterSPI makeNestedPolyMultiLevel(AvroEventPropertyGetter[] getters);
    }

    private static class GetterNestedFactoryRootedSimple implements GetterNestedFactory {
        private final EventAdapterService eventAdapterService;
        private final int posTop;

        public GetterNestedFactoryRootedSimple(EventAdapterService eventAdapterService, int posTop) {
            this.eventAdapterService = eventAdapterService;
            this.posTop = posTop;
        }

        public EventPropertyGetterSPI makeSimple(int posNested, EventType fragmentEventType, Class propertyType) {
            return new AvroEventBeanGetterNestedSimple(posTop, posNested, fragmentEventType, eventAdapterService);
        }

        public EventPropertyGetterSPI makeIndexed(int posNested, int index, EventType fragmentEventType) {
            return new AvroEventBeanGetterNestedIndexed(posTop, posNested, index, fragmentEventType, eventAdapterService);
        }

        public EventPropertyGetterSPI makeMapped(int posNested, String key) {
            return new AvroEventBeanGetterNestedMapped(posTop, posNested, key);
        }

        public EventPropertyGetterSPI makeDynamicSimple(String propertyName) {
            return new AvroEventBeanGetterNestedDynamicSimple(posTop, propertyName);
        }

        public EventPropertyGetterSPI makeNestedSimpleMultiLevel(int[] path, Class[] propertyTypes, EventType fragmentEventType) {
            return new AvroEventBeanGetterNestedMultiLevel(posTop, path, fragmentEventType, eventAdapterService);
        }

        public EventPropertyGetterSPI makeNestedPolyMultiLevel(AvroEventPropertyGetter[] getters) {
            return new AvroEventBeanGetterNestedPoly(posTop, getters);
        }
    }

    private static class GetterNestedFactoryRootedIndexed implements GetterNestedFactory {
        private final EventAdapterService eventAdapterService;
        private final int pos;
        private final int index;

        public GetterNestedFactoryRootedIndexed(EventAdapterService eventAdapterService, int pos, int index) {
            this.eventAdapterService = eventAdapterService;
            this.pos = pos;
            this.index = index;
        }

        public EventPropertyGetterSPI makeSimple(int posNested, EventType fragmentEventType, Class propertyType) {
            return new AvroEventBeanGetterNestedIndexRooted(pos, index, new AvroEventBeanGetterSimple(posNested, fragmentEventType, eventAdapterService, propertyType));
        }

        public EventPropertyGetterSPI makeIndexed(int posNested, int index, EventType fragmentEventType) {
            return new AvroEventBeanGetterNestedIndexRooted(pos, index, new AvroEventBeanGetterIndexed(posNested, index, fragmentEventType, eventAdapterService));
        }

        public EventPropertyGetterSPI makeMapped(int posNested, String key) {
            return new AvroEventBeanGetterNestedIndexRooted(pos, index, new AvroEventBeanGetterMapped(posNested, key));
        }

        public EventPropertyGetterSPI makeDynamicSimple(String propertyName) {
            return new AvroEventBeanGetterNestedIndexRooted(pos, index, new AvroEventBeanGetterSimpleDynamic(propertyName));
        }

        public EventPropertyGetterSPI makeNestedSimpleMultiLevel(int[] path, Class[] propertyTypes, EventType fragmentEventType) {
            AvroEventPropertyGetter[] getters = new AvroEventPropertyGetter[path.length];
            for (int i = 0; i < path.length; i++) {
                getters[i] = new AvroEventBeanGetterSimple(path[i], fragmentEventType, eventAdapterService, propertyTypes[i]);
            }
            return new AvroEventBeanGetterNestedIndexRootedMultilevel(pos, index, getters);
        }

        public EventPropertyGetterSPI makeNestedPolyMultiLevel(AvroEventPropertyGetter[] getters) {
            return new AvroEventBeanGetterNestedIndexRootedMultilevel(pos, index, getters);
        }
    }
}
