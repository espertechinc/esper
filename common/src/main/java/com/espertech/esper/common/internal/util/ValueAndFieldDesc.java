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

public class ValueAndFieldDesc {
    private final Object value;
    private final Field field;

    public ValueAndFieldDesc(Object value, Field field) {
        this.value = value;
        this.field = field;
    }

    public Object getValue() {
        return value;
    }

    public Field getField() {
        return field;
    }
}
