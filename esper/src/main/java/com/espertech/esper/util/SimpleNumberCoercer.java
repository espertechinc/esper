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
 * Interface for number coercion.
 */
public interface SimpleNumberCoercer {
    /**
     * Coerce the given number to a previously determined type, assuming the type is a Boxed type. Allows coerce to lower resultion number.
     * Does't coerce to primitive types.
     *
     * @param numToCoerce is the number to coerce to the given type
     * @return the numToCoerce as a value in the given result type
     */
    public Number coerceBoxed(Number numToCoerce);

    public Class getReturnType();
}
