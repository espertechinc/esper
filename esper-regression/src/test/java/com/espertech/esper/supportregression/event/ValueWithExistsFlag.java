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
package com.espertech.esper.supportregression.event;

public class ValueWithExistsFlag {
    private final boolean exists;
    private final Object value;

    private ValueWithExistsFlag(boolean exists, Object value) {
        this.exists = exists;
        this.value = value;
    }

    public boolean isExists() {
        return exists;
    }

    public Object getValue() {
        return value;
    }

    public static ValueWithExistsFlag notExists() {
        return new ValueWithExistsFlag(false, null);
    }

    public static ValueWithExistsFlag exists(Object value) {
        return new ValueWithExistsFlag(true, value);
    }

    public static ValueWithExistsFlag[] multipleNotExists(int count) {
        ValueWithExistsFlag[] flagged = new ValueWithExistsFlag[count];
        for (int i = 0; i < flagged.length; i++) {
            flagged[i] = notExists();
        }
        return flagged;
    }

    public static ValueWithExistsFlag[] allExist(Object... values) {
        ValueWithExistsFlag[] flagged = new ValueWithExistsFlag[values.length];
        for (int i = 0; i < values.length; i++) {
            flagged[i] = exists(values[i]);
        }
        return flagged;
    }
}
