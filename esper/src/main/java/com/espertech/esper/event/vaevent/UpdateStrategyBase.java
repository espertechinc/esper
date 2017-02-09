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
package com.espertech.esper.event.vaevent;

import com.espertech.esper.util.NullableObject;

/**
 * Base strategy implementation holds the specification object.
 */
public abstract class UpdateStrategyBase implements UpdateStrategy {
    /**
     * The specification.
     */
    protected final RevisionSpec spec;

    /**
     * Ctor.
     *
     * @param spec is the specification
     */
    protected UpdateStrategyBase(RevisionSpec spec) {
        this.spec = spec;
    }

    /**
     * Array copy.
     *
     * @param array to copy
     * @return copied array
     */
    protected static NullableObject<Object>[] arrayCopy(NullableObject<Object>[] array) {
        if (array == null) {
            return null;
        }
        NullableObject<Object>[] result = (NullableObject<Object>[]) new NullableObject[array.length];
        System.arraycopy(array, 0, result, 0, array.length);
        return result;
    }
}
