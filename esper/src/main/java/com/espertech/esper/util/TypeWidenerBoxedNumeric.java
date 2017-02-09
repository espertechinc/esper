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
package com.espertech.esper.util;

/**
 * Widerner that coerces to a widened boxed number.
 */
public class TypeWidenerBoxedNumeric implements TypeWidener {
    private final SimpleNumberCoercer coercer;

    /**
     * Ctor.
     *
     * @param coercer the coercer
     */
    public TypeWidenerBoxedNumeric(SimpleNumberCoercer coercer) {
        this.coercer = coercer;
    }

    public Object widen(Object input) {
        return coercer.coerceBoxed((Number) input);
    }
}
