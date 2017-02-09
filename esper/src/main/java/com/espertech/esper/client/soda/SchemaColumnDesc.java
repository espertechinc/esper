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
package com.espertech.esper.client.soda;

import java.io.Serializable;
import java.io.StringWriter;

/**
 * Descriptor for use in create-schema syntax to define property name and type of an event property.
 */
public class SchemaColumnDesc implements Serializable {
    private static final long serialVersionUID = 5068685531968720148L;

    private String name;
    private String type;
    private boolean array;
    private boolean primitiveArray;

    /**
     * Ctor.
     */
    public SchemaColumnDesc() {
    }

    /**
     * Ctor.
     *
     * @param name  column name
     * @param type  type name
     * @param array array flag
     */
    public SchemaColumnDesc(String name, String type, boolean array) {
        this.name = name;
        this.type = type;
        this.array = array;
    }

    /**
     * Ctor.
     *
     * @param name           property name
     * @param type           property type, can be any simple class name or fully-qualified class name or existing event type name
     * @param array          true for array property
     * @param primitiveArray true for array of primitive (requires array property to be set and a primitive type)
     */
    public SchemaColumnDesc(String name, String type, boolean array, boolean primitiveArray) {
        this.name = name;
        this.type = type;
        this.array = array;
        this.primitiveArray = primitiveArray;
    }

    /**
     * Returns property name.
     *
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns property type.
     *
     * @return type
     */
    public String getType() {
        return type;
    }

    /**
     * Returns true for array properties.
     *
     * @return indicator
     */
    public boolean isArray() {
        return array;
    }

    /**
     * Set property name.
     *
     * @param name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Set property type.
     *
     * @param type type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Set array indicator.
     *
     * @param array indicator
     */
    public void setArray(boolean array) {
        this.array = array;
    }

    /**
     * Returns indicator whether array of primitives (requires array and a primitive type)
     *
     * @return indicator
     */
    public boolean isPrimitiveArray() {
        return primitiveArray;
    }

    /**
     * Sets indicator whether array of primitives (requires array and a primitive type)
     *
     * @param primitiveArray indicator
     */
    public void setPrimitiveArray(boolean primitiveArray) {
        this.primitiveArray = primitiveArray;
    }

    /**
     * Render to EPL.
     *
     * @param writer to render to
     */
    public void toEPL(StringWriter writer) {
        writer.write(name);
        writer.write(' ');
        writer.write(type);
        if (array) {
            if (primitiveArray) {
                writer.write("[primitive]");
            } else {
                writer.write("[]");
            }
        }
    }

}
