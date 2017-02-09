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
 * Count-min sketch base context object.
 */
public abstract class CountMinSketchAgentContext {
    private final CountMinSketchState state;

    /**
     * Ctor.
     *
     * @param state the state
     */
    protected CountMinSketchAgentContext(CountMinSketchState state) {
        this.state = state;
    }

    /**
     * Returns state
     *
     * @return state
     */
    public CountMinSketchState getState() {
        return state;
    }
}
