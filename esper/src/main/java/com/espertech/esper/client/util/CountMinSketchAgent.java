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
package com.espertech.esper.client.util;

/**
 * For use with Count-min sketch aggregation functions:
 * The agent implementation encapsulates transformation of value objects to byte-array and back (when needed),
 * and may override or provide custom behavior.
 */
public interface CountMinSketchAgent {
    /**
     * Returns an array of types that the agent can handle, for validation purposes.
     * For example, an agent that accepts byte-array type values should return "new Class[] {String.class}".
     * Interfaces and supertype classes can also be part of the class array.
     *
     * @return class array of acceptable type
     */
    public Class[] getAcceptableValueTypes();

    /**
     * Add a value to the Count-min sketch.
     * Implementations typically check for null value, convert the value object to a byte-array
     * and invoke a method on the state object to add the byte-array value.
     *
     * @param ctx contains value to add as well as the state
     */
    public void add(CountMinSketchAgentContextAdd ctx);

    /**
     * Return the estimated count for a given value.
     * Implementations typically check for null value, convert the value object to a byte-array
     * and invoke a method on the state object to retrieve a count.
     *
     * @param ctx contains value to query as well as the state
     * @return estimated count
     */
    public Long estimate(CountMinSketchAgentContextEstimate ctx);

    /**
     * Return the value object for a given byte-array, for use with top-K.
     * Implementations typically simply convert a byte-array into a value object.
     *
     * @param ctx value object and state
     * @return value object
     */
    public Object fromBytes(CountMinSketchAgentContextFromBytes ctx);
}
