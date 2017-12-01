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
package com.espertech.esper.epl.spec;

import java.io.Serializable;

/**
 * Describes a column name and type.
 */
public class ColumnDesc implements Serializable {
    private static final long serialVersionUID = -3508097717971934622L;

    private final String name;
    private final String type;
    private final boolean array;
    private final boolean primitiveArray;

    /**
     * Ctor.
     *
     * @param name           column name
     * @param type           type
     * @param array          true for array
     * @param primitiveArray true for array of primitives
     */
    public ColumnDesc(String name, String type, boolean array, boolean primitiveArray) {
        this.name = name;
        this.type = type;
        this.array = array;
        this.primitiveArray = primitiveArray;
    }

    /**
     * Returns column name.
     *
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Return column type
     *
     * @return type
     */
    public String getType() {
        return type;
    }

    /**
     * Return true for array
     *
     * @return array indicator
     */
    public boolean isArray() {
        return array;
    }

    public boolean isPrimitiveArray() {
        return primitiveArray;
    }
}
