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
package com.espertech.esper.type;

/**
 * Enumeration of types of primitive values.
 */
public enum PrimitiveValueType {
    /**
     * Byte.
     */
    BYTE("byte"),

    /**
     * Short.
     */
    SHORT("short"),

    /**
     * Integer.
     */
    INTEGER("int"),

    /**
     * Long.
     */
    LONG("long"),

    /**
     * Float.
     */
    FLOAT("float"),

    /**
     * Double.
     */
    DOUBLE("double"),

    /**
     * Boolean.
     */
    BOOL("bool"),

    /**
     * String.
     */
    STRING("string");

    private String typeName;

    private PrimitiveValueType(String typeName) {
        this.typeName = typeName;
    }

    /**
     * Returns the name of the type.
     *
     * @return type name
     */
    public String getTypeName() {
        return typeName;
    }

    public String toString() {
        return typeName;
    }
}
