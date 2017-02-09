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
package com.espertech.esper.event;

import java.lang.reflect.Method;

/**
 * Descriptor for writable properties.
 */
public class WriteablePropertyDescriptor {
    private String propertyName;
    private Class type;
    private Method writeMethod;

    /**
     * Ctor.
     *
     * @param propertyName name of property
     * @param type         type
     * @param writeMethod  optional write methods
     */
    public WriteablePropertyDescriptor(String propertyName, Class type, Method writeMethod) {
        this.propertyName = propertyName;
        this.type = type;
        this.writeMethod = writeMethod;
    }

    /**
     * Returns property name.
     *
     * @return property name
     */
    public String getPropertyName() {
        return propertyName;
    }

    /**
     * Returns property type.
     *
     * @return property type
     */
    public Class getType() {
        return type;
    }

    /**
     * Returns write methods.
     *
     * @return write methods
     */
    public Method getWriteMethod() {
        return writeMethod;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        WriteablePropertyDescriptor that = (WriteablePropertyDescriptor) o;

        if (!propertyName.equals(that.propertyName)) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        return propertyName.hashCode();
    }
}
