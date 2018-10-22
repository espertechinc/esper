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
package com.espertech.esperio.csv;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

/**
 * Coercer for type conversion.
 */
public abstract class AbstractTypeCoercer {

    /**
     * For logging.
     */
    protected static final Logger log = LoggerFactory.getLogger(AbstractTypeCoercer.class);

    /**
     * Constructors for coercion.
     */
    protected Map<String, Constructor> propertyConstructors;

    /**
     * Ctor.
     *
     * @param propertyTypes the type conversion to be done
     */
    public void setPropertyTypes(Map<String, Object> propertyTypes) {
        this.propertyConstructors = createPropertyConstructors(propertyTypes);
    }

    /**
     * Convert a value.
     *
     * @param property property name
     * @param source   value to convert
     * @return object value
     * @throws Exception if coercion failed
     */
    abstract Object coerce(String property, String source) throws Exception;

    private Map<String, Constructor> createPropertyConstructors(Map<String, Object> propertyTypes) {
        Map<String, Constructor> constructors = new HashMap<String, Constructor>();

        Class[] parameterTypes = new Class[]{String.class};
        for (String property : propertyTypes.keySet()) {
            log.debug(".createPropertyConstructors property==" + property + ", type==" + propertyTypes.get(property));
            Class clazz = JavaClassHelper.getBoxedType((Class) propertyTypes.get(property));
            Constructor constructor = null;
            try {
                constructor = clazz.getConstructor(parameterTypes);
            } catch (NoSuchMethodException e) {
                throw new EPException("Failed to find constructure for class " + clazz.getName() + " taking " + JavaClassHelper.getParameterAsString(parameterTypes));
            }
            constructors.put(property, constructor);
        }
        return constructors;
    }
}
