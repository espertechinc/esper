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
package com.espertech.esper.common.internal.avro.core;

import com.espertech.esper.common.client.FragmentEventType;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonEventTypeAvro;
import com.espertech.esper.common.client.meta.EventTypeApplicationType;
import com.espertech.esper.common.client.meta.EventTypeIdPair;
import com.espertech.esper.common.client.meta.EventTypeMetadata;
import com.espertech.esper.common.client.meta.EventTypeTypeClass;
import com.espertech.esper.common.client.util.EventTypeBusModifier;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.event.avro.AvroSchemaEventType;
import com.espertech.esper.common.internal.event.avro.EventTypeAvroHandler;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.event.core.PropertySetDescriptorItem;
import com.espertech.esper.common.internal.event.property.Property;
import com.espertech.esper.common.internal.event.property.PropertyParser;
import com.espertech.esper.common.internal.util.StringValue;
import org.apache.avro.Schema;

import java.util.Map;

public class AvroFragmentTypeUtil {
    protected static FragmentEventType getFragmentType(Schema schema, String propertyName, String moduleName, Map<String, PropertySetDescriptorItem> propertyItems, EventBeanTypedEventFactory eventBeanTypedEventFactory, EventTypeAvroHandler eventTypeAvroHandler, AvroEventTypeFragmentTypeCache fragmentTypeCache) {
        String unescapePropName = StringValue.unescapeDot(propertyName);
        PropertySetDescriptorItem item = propertyItems.get(unescapePropName);
        if (item != null) {
            return item.getFragmentEventType();
        }

        Property property = PropertyParser.parseAndWalkLaxToSimple(propertyName);
        AvroFieldDescriptor desc = AvroFieldUtil.fieldForProperty(schema, property);
        if (desc == null) {
            return null;
        }
        if (desc.isDynamic()) {
            return null;
        }
        Schema fieldSchemaByAccess = desc.getField().schema();
        if (desc.isAccessedByIndex()) {
            fieldSchemaByAccess = fieldSchemaByAccess.getElementType();
        }
        return getFragmentEventTypeForField(fieldSchemaByAccess, moduleName, eventBeanTypedEventFactory, eventTypeAvroHandler, fragmentTypeCache);
    }

    protected static FragmentEventType getFragmentEventTypeForField(Schema fieldSchema, String moduleName, EventBeanTypedEventFactory eventBeanTypedEventFactory, EventTypeAvroHandler eventTypeAvroHandler, AvroEventTypeFragmentTypeCache fragmentTypeCache) {
        Schema recordSchema;
        boolean indexed = false;
        if (fieldSchema.getType() == Schema.Type.RECORD) {
            recordSchema = fieldSchema;
        } else if (fieldSchema.getType() == Schema.Type.ARRAY && fieldSchema.getElementType().getType() == Schema.Type.RECORD) {
            recordSchema = fieldSchema.getElementType();
            indexed = true;
        } else {
            return null;
        }

        AvroSchemaEventType cached = fragmentTypeCache.get(recordSchema.getName());
        if (cached != null) {
            return new FragmentEventType(cached, indexed, false);
        }

        EventTypeMetadata metadata = new EventTypeMetadata(recordSchema.getName(), moduleName, EventTypeTypeClass.STREAM, EventTypeApplicationType.AVRO, NameAccessModifier.TRANSIENT, EventTypeBusModifier.NONBUS, false, EventTypeIdPair.unassigned());
        ConfigurationCommonEventTypeAvro config = new ConfigurationCommonEventTypeAvro();
        config.setAvroSchema(recordSchema);

        AvroSchemaEventType fragmentType = eventTypeAvroHandler.newEventTypeFromSchema(metadata, eventBeanTypedEventFactory, config, null, null);

        fragmentTypeCache.add(recordSchema.getName(), fragmentType);
        return new FragmentEventType(fragmentType, indexed, false);
    }
}
