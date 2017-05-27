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
package com.espertech.esper.supportregression.bean;

import com.espertech.esper.client.EventBean;

public enum SupportEnumTwo {
    ENUM_VALUE_1(100, new String[]{"1", "0", "0"}),
    ENUM_VALUE_2(200, new String[]{"2", "0", "0"}),
    ENUM_VALUE_3(300, new String[]{"3", "0", "0"});

    private final int associatedValue;
    private final String[] mystrings;

    private SupportEnumTwo(int associatedValue, String[] mystrings) {
        this.associatedValue = associatedValue;
        this.mystrings = mystrings;
    }

    public String[] getMystrings() {
        return mystrings;
    }

    public int getAssociatedValue() {
        return associatedValue;
    }

    public boolean checkAssociatedValue(int value) {
        return this.associatedValue == value;
    }

    public boolean checkEventBeanPropInt(EventBean event, String propertyName) {
        Object value = event.get(propertyName);
        if (value == null && (!(value instanceof Integer))) {
            return false;
        }
        return associatedValue == (Integer) value;
    }

    public Nested getNested() {
        return new Nested(associatedValue);
    }

    public static class Nested {
        private final int value;

        public Nested(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }
}
