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

import com.espertech.esper.epl.approx.CountMinSketchState;

/**
 * Count-min sketch context object for estimate-operations.
 */
public class CountMinSketchAgentContextEstimate extends CountMinSketchAgentContext {
    private Object value;

    /**
     * Ctor.
     *
     * @param state the state
     */
    public CountMinSketchAgentContextEstimate(CountMinSketchState state) {
        super(state);
    }

    /**
     * Returns the value.
     *
     * @return value
     */
    public Object getValue() {
        return value;
    }

    /**
     * Sets the value.
     *
     * @param value to set
     */
    public void setValue(Object value) {
        this.value = value;
    }
}
