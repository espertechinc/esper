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
package com.espertech.esper.common.client.hook.enummethod;

/**
 * Interface for state-providing classes for use with enumeration method extension
 */
public interface EnumMethodState {
    /**
     * Called by the runtime to provide non-lambda expression parameter values
     * @param parameterNumber zero for the first parameter, reflects parameter position
     * @param value parameter value or null if the parameter expression returned null
     */
    default void setParameter(int parameterNumber, Object value) {
    }

    /**
     * Called by the runtime only if during compile-time the mode indicated early-exit.
     * @return indicator, true for done, false for more
     */
    default boolean completed() {
        return false;
    }

    /**
     * Returns the enumeration result
     * @return result
     */
    Object state();
}
