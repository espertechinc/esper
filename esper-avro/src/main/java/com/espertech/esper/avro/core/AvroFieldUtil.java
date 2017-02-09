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

import com.espertech.esper.event.property.*;
import org.apache.avro.Schema;

public class AvroFieldUtil {
    protected static AvroFieldDescriptor fieldForProperty(Schema fieldSchema, Property property) {
        if (property instanceof SimpleProperty) {
            Schema.Field field = fieldSchema.getField(property.getPropertyNameAtomic());
            if (field == null) {
                return null;
            }
            return new AvroFieldDescriptor(field, false, false, false);
        } else if (property instanceof IndexedProperty) {
            Schema.Field field = fieldSchema.getField(property.getPropertyNameAtomic());
            if (field == null || field.schema().getType() != Schema.Type.ARRAY) {
                return null;
            }
            return new AvroFieldDescriptor(field, false, true, false);
        } else if (property instanceof MappedProperty) {
            Schema.Field field = fieldSchema.getField(property.getPropertyNameAtomic());
            if (field == null || field.schema().getType() != Schema.Type.MAP) {
                return null;
            }
            return new AvroFieldDescriptor(field, false, false, true);
        } else if (property instanceof DynamicProperty) {
            Schema.Field field = fieldSchema.getField(property.getPropertyNameAtomic());
            return new AvroFieldDescriptor(field, true, property instanceof DynamicIndexedProperty, property instanceof DynamicMappedProperty);
        }

        NestedProperty nested = (NestedProperty) property;
        Schema current = fieldSchema;
        Schema.Field currentField = null;
        boolean dynamic = false;
        for (int index = 0; index < nested.getProperties().size(); index++) {
            Property levelProperty = nested.getProperties().get(index);
            if (levelProperty instanceof SimpleProperty) {
                if (current.getType() != Schema.Type.RECORD) {
                    return null;
                }
                currentField = current.getField(levelProperty.getPropertyNameAtomic());
                if (currentField == null) {
                    return null;
                }
                current = currentField.schema();
            } else if (levelProperty instanceof IndexedProperty) {
                if (current.getType() != Schema.Type.RECORD) {
                    return null;
                }
                currentField = current.getField(levelProperty.getPropertyNameAtomic());
                if (currentField == null || currentField.schema().getType() != Schema.Type.ARRAY) {
                    return null;
                }
                current = currentField.schema().getElementType();
            } else if (levelProperty instanceof MappedProperty) {
                if (current.getType() != Schema.Type.RECORD) {
                    return null;
                }
                currentField = current.getField(levelProperty.getPropertyNameAtomic());
                if (currentField == null || currentField.schema().getType() != Schema.Type.MAP) {
                    return null;
                }
                current = currentField.schema().getValueType();
            } else if (levelProperty instanceof DynamicProperty) {
                dynamic = true;
                currentField = fieldSchema.getField(levelProperty.getPropertyNameAtomic());
                if (currentField == null) {
                    return new AvroFieldDescriptor(null, true, levelProperty instanceof DynamicIndexedProperty, levelProperty instanceof DynamicMappedProperty);
                }
                current = currentField.schema();
            }
        }
        Property lastProperty = nested.getProperties().get(nested.getProperties().size() - 1);
        return new AvroFieldDescriptor(currentField, dynamic, lastProperty instanceof PropertyWithIndex, lastProperty instanceof PropertyWithKey);
    }
}
