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
package com.espertech.esper.epl.expression.core;

public class PopulateFieldWValueDescriptor {
    private final String propertyName;
    private final Class fieldType;
    private final Class containerType;
    private final PopulateFieldValueSetter setter;
    private final boolean forceNumeric;

    public PopulateFieldWValueDescriptor(String propertyName, Class fieldType, Class containerType, PopulateFieldValueSetter setter, boolean forceNumeric) {
        this.propertyName = propertyName;
        this.fieldType = fieldType;
        this.containerType = containerType;
        this.setter = setter;
        this.forceNumeric = forceNumeric;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public Class getFieldType() {
        return fieldType;
    }

    public Class getContainerType() {
        return containerType;
    }

    public PopulateFieldValueSetter getSetter() {
        return setter;
    }

    public boolean isForceNumeric() {
        return forceNumeric;
    }
}
