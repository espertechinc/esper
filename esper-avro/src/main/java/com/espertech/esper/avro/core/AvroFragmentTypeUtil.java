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

import com.espertech.esper.client.ConfigurationEventTypeAvro;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.FragmentEventType;
import com.espertech.esper.epl.parse.ASTUtil;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.PropertySetDescriptorItem;
import com.espertech.esper.event.property.Property;
import com.espertech.esper.event.property.PropertyParser;
import org.apache.avro.Schema;

import java.util.Map;

public class AvroFragmentTypeUtil {
    protected static FragmentEventType getFragmentType(Schema schema, String propertyName, Map<String, PropertySetDescriptorItem> propertyItems, EventAdapterService eventAdapterService) {
        String unescapePropName = ASTUtil.unescapeDot(propertyName);
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
        return getFragmentEventTypeForField(fieldSchemaByAccess, eventAdapterService);
    }

    protected static FragmentEventType getFragmentEventTypeForField(Schema fieldSchema, EventAdapterService eventAdapterService) {
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

        // See if there is an existing type
        EventType existing = eventAdapterService.getExistsTypeByName(recordSchema.getName());
        if (existing != null && existing instanceof AvroEventType) {
            return new FragmentEventType(existing, indexed, false);
        }

        EventType fragmentType = eventAdapterService.addAvroType(recordSchema.getName(), new ConfigurationEventTypeAvro().setAvroSchema(recordSchema), false, false, false, false, false);
        return new FragmentEventType(fragmentType, indexed, false);
    }
}
