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
package com.espertech.esper.common.internal.epl.expression.core;

import com.espertech.esper.common.client.type.EPTypeClass;

public class PopulateFieldWValueDescriptor {
    private final String propertyName;
    private final EPTypeClass fieldType;
    private final EPTypeClass containerType;
    private final PopulateFieldValueSetter setter;
    private final boolean forceNumeric;

    public PopulateFieldWValueDescriptor(String propertyName, EPTypeClass fieldType, EPTypeClass containerType, PopulateFieldValueSetter setter, boolean forceNumeric) {
        this.propertyName = propertyName;
        this.fieldType = fieldType;
        this.containerType = containerType;
        this.setter = setter;
        this.forceNumeric = forceNumeric;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public EPTypeClass getFieldType() {
        return fieldType;
    }

    public EPTypeClass getContainerType() {
        return containerType;
    }

    public PopulateFieldValueSetter getSetter() {
        return setter;
    }

    public boolean isForceNumeric() {
        return forceNumeric;
    }
}
