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
package com.espertech.esper.common.internal.util;

import java.lang.reflect.Field;

public class EnumValue {
    private final Class enumClass;
    private final Field enumField;

    public EnumValue(Class enumClass, Field enumField) {
        this.enumClass = enumClass;
        this.enumField = enumField;
    }

    public Class getEnumClass() {
        return enumClass;
    }

    public Field getEnumField() {
        return enumField;
    }
}
