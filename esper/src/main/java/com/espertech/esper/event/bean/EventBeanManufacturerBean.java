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
package com.espertech.esper.event.bean;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.EventBeanManufactureException;
import com.espertech.esper.event.EventBeanManufacturer;
import com.espertech.esper.event.WriteablePropertyDescriptor;
import net.sf.cglib.reflect.FastMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Factory for event beans created and populate anew from a set of values.
 */
public class EventBeanManufacturerBean implements EventBeanManufacturer {
    private final static Logger log = LoggerFactory.getLogger(EventBeanManufacturerBean.class);

    private final BeanInstantiator beanInstantiator;
    private final BeanEventType beanEventType;
    private final EventAdapterService service;
    private final FastMethod[] writeMethodsFastClass;
    private final Method[] writeMethodsReflection;
    private final boolean hasPrimitiveTypes;
    private final boolean[] primitiveType;

    /**
     * Ctor.
     *
     * @param beanEventType       target type
     * @param service             factory for events
     * @param properties          written properties
     * @param engineImportService for resolving write methods
     * @throws EventBeanManufactureException if the write method lookup fail
     */
    public EventBeanManufacturerBean(BeanEventType beanEventType,
                                     EventAdapterService service,
                                     WriteablePropertyDescriptor[] properties,
                                     EngineImportService engineImportService
    )
            throws EventBeanManufactureException {
        this.beanEventType = beanEventType;
        this.service = service;

        beanInstantiator = BeanInstantiatorFactory.makeInstantiator(beanEventType, engineImportService);

        writeMethodsReflection = new Method[properties.length];
        if (beanEventType.getFastClass() != null) {
            writeMethodsFastClass = new FastMethod[properties.length];
        } else {
            writeMethodsFastClass = null;
        }

        boolean primitiveTypeCheck = false;
        primitiveType = new boolean[properties.length];
        for (int i = 0; i < properties.length; i++) {
            writeMethodsReflection[i] = properties[i].getWriteMethod();
            if (beanEventType.getFastClass() != null) {
                writeMethodsFastClass[i] = beanEventType.getFastClass().getMethod(properties[i].getWriteMethod());
            }

            primitiveType[i] = properties[i].getType().isPrimitive();
            primitiveTypeCheck |= primitiveType[i];
        }
        hasPrimitiveTypes = primitiveTypeCheck;
    }

    public EventBean make(Object[] propertyValues) {
        Object outObject = makeUnderlying(propertyValues);
        return service.adapterForTypedBean(outObject, beanEventType);
    }

    public Object makeUnderlying(Object[] propertyValues) {
        Object outObject = beanInstantiator.instantiate();

        if (writeMethodsFastClass != null) {
            if (!hasPrimitiveTypes) {
                Object[] parameters = new Object[1];
                for (int i = 0; i < writeMethodsFastClass.length; i++) {
                    parameters[0] = propertyValues[i];
                    try {
                        writeMethodsFastClass[i].invoke(outObject, parameters);
                    } catch (InvocationTargetException e) {
                        handle(e, writeMethodsFastClass[i].getName());
                    }
                }
            } else {
                Object[] parameters = new Object[1];
                for (int i = 0; i < writeMethodsFastClass.length; i++) {
                    if (primitiveType[i]) {
                        if (propertyValues[i] == null) {
                            continue;
                        }
                    }
                    parameters[0] = propertyValues[i];
                    try {
                        writeMethodsFastClass[i].invoke(outObject, parameters);
                    } catch (InvocationTargetException e) {
                        handle(e, writeMethodsFastClass[i].getName());
                    }
                }
            }
        } else {

            if (!hasPrimitiveTypes) {
                Object[] parameters = new Object[1];
                for (int i = 0; i < writeMethodsReflection.length; i++) {
                    parameters[0] = propertyValues[i];
                    try {
                        writeMethodsReflection[i].invoke(outObject, parameters);
                    } catch (InvocationTargetException e) {
                        String message = "Unexpected exception encountered invoking setter-method '" + writeMethodsReflection[i] + "' on class '" +
                                beanEventType.getUnderlyingType().getName() + "' : " + e.getTargetException().getMessage();
                        log.error(message, e);
                    } catch (IllegalAccessException e) {
                        handle(e, writeMethodsReflection[i].getName());
                    }
                }
            } else {
                Object[] parameters = new Object[1];
                for (int i = 0; i < writeMethodsReflection.length; i++) {
                    if (primitiveType[i]) {
                        if (propertyValues[i] == null) {
                            continue;
                        }
                    }
                    parameters[0] = propertyValues[i];
                    try {
                        writeMethodsReflection[i].invoke(outObject, parameters);
                    } catch (InvocationTargetException e) {
                        handle(e, writeMethodsReflection[i].getName());
                    } catch (IllegalAccessException e) {
                        handle(e, writeMethodsReflection[i].getName());
                    }
                }
            }
        }

        return outObject;
    }

    private void handle(InvocationTargetException ex, String methodName) {
        String message = "Unexpected exception encountered invoking setter-method '" + methodName + "' on class '" +
                beanEventType.getUnderlyingType().getName() + "' : " + ex.getTargetException().getMessage();
        log.error(message, ex);
    }

    private void handle(IllegalAccessException ex, String methodName) {
        String message = "Unexpected exception encountered invoking setter-method '" + methodName + "' on class '" +
                beanEventType.getUnderlyingType().getName() + "' : " + ex.getMessage();
        log.error(message, ex);
    }
}
