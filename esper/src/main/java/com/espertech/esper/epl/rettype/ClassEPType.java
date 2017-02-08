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
package com.espertech.esper.epl.rettype;

/**
 * Any Java primitive type as well as any class and other non-array or non-collection type
 */
public class ClassEPType implements EPType {
    private final Class type;

    protected ClassEPType(Class type) {
        this.type = type;
    }

    public Class getType() {
        return type;
    }
}
