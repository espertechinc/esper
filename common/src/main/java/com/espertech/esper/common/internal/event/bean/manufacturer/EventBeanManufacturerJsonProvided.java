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
package com.espertech.esper.common.internal.event.bean.manufacturer;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.event.bean.instantiator.BeanInstantiator;
import com.espertech.esper.common.internal.event.bean.instantiator.BeanInstantiatorForgeByNewInstanceReflection;
import com.espertech.esper.common.internal.event.core.EventBeanManufactureException;
import com.espertech.esper.common.internal.event.core.EventBeanManufacturer;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.event.core.WriteablePropertyDescriptor;
import com.espertech.esper.common.internal.event.json.compiletime.JsonUnderlyingField;
import com.espertech.esper.common.internal.event.json.core.JsonEventType;
import com.espertech.esper.common.internal.settings.ClasspathImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

/**
 * Factory for event beans created and populate anew from a set of values.
 */
public class EventBeanManufacturerJsonProvided implements EventBeanManufacturer {
    private final static Logger log = LoggerFactory.getLogger(EventBeanManufacturerJsonProvided.class);

    private final BeanInstantiator beanInstantiator;
    private final JsonEventType jsonEventType;
    private final EventBeanTypedEventFactory service;
    private final Field[] writeFieldsReflection;
    private final boolean hasPrimitiveTypes;
    private final boolean[] primitiveType;

    /**
     * Ctor.
     *
     * @param jsonEventType          target type
     * @param service                factory for events
     * @param properties             written properties
     * @param classpathImportService for resolving write methods
     * @throws EventBeanManufactureException if the write method lookup fail
     */
    public EventBeanManufacturerJsonProvided(JsonEventType jsonEventType,
                                             EventBeanTypedEventFactory service,
                                             WriteablePropertyDescriptor[] properties,
                                             ClasspathImportService classpathImportService
    )
        throws EventBeanManufactureException {
        this.jsonEventType = jsonEventType;
        this.service = service;

        beanInstantiator = new BeanInstantiatorForgeByNewInstanceReflection(jsonEventType.getUnderlyingType());

        writeFieldsReflection = new Field[properties.length];
        boolean primitiveTypeCheck = false;
        primitiveType = new boolean[properties.length];
        for (int i = 0; i < properties.length; i++) {
            String propertyName = properties[i].getPropertyName();
            JsonUnderlyingField field = jsonEventType.getDetail().getFieldDescriptors().get(propertyName);
            writeFieldsReflection[i] = field.getOptionalField();
            primitiveType[i] = properties[i].getType().isPrimitive();
            primitiveTypeCheck |= primitiveType[i];
        }
        hasPrimitiveTypes = primitiveTypeCheck;
    }

    public EventBean make(Object[] propertyValues) {
        Object outObject = makeUnderlying(propertyValues);
        return service.adapterForTypedJson(outObject, jsonEventType);
    }

    public Object makeUnderlying(Object[] propertyValues) {
        Object outObject = beanInstantiator.instantiate();

        if (!hasPrimitiveTypes) {
            for (int i = 0; i < writeFieldsReflection.length; i++) {
                try {
                    writeFieldsReflection[i].set(outObject, propertyValues[i]);
                } catch (IllegalAccessException e) {
                    handle(e, writeFieldsReflection[i].getName());
                }
            }
        } else {
            for (int i = 0; i < writeFieldsReflection.length; i++) {
                if (primitiveType[i]) {
                    if (propertyValues[i] == null) {
                        continue;
                    }
                }
                try {
                    writeFieldsReflection[i].set(outObject, propertyValues[i]);
                } catch (IllegalAccessException e) {
                    handle(e, writeFieldsReflection[i].getName());
                }
            }
        }
        return outObject;
    }

    private void handle(IllegalAccessException ex, String fieldName) {
        String message = "Unexpected exception encountered invoking setter for field '" + fieldName + "' on class '" +
            jsonEventType.getUnderlyingType().getName() + "' : " + ex.getMessage();
        log.error(message, ex);
    }
}
