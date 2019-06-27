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
package com.espertech.esper.common.internal.event.bean.core;

import java.lang.reflect.Field;

/**
 * Provides method information for dynamic (unchecked) properties of each class for use in obtaining property values.
 */
public class DynamicPropertyDescriptorByField {
    private Class clazz;
    private Field field;

    /**
     * Ctor.
     *
     * @param clazz         the class to match when looking for a method
     * @param field        the field to use
     */
    public DynamicPropertyDescriptorByField(Class clazz, Field field) {
        this.clazz = clazz;
        this.field = field;
    }

    /**
     * Returns the class for the method.
     *
     * @return class to match on
     */
    public Class getClazz() {
        return clazz;
    }

    /**
     * Returns the field.
     *
     * @return field to use
     */
    public Field getField() {
        return field;
    }
}
