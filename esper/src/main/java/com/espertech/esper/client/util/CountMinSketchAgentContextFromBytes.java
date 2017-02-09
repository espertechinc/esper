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
 * Count-min sketch context object for topk-operations.
 */
public class CountMinSketchAgentContextFromBytes extends CountMinSketchAgentContext {
    private byte[] bytes;

    /**
     * Ctor.
     *
     * @param state the state
     */
    public CountMinSketchAgentContextFromBytes(CountMinSketchState state) {
        super(state);
    }

    /**
     * Returns the byte value.
     *
     * @return bytes
     */
    public byte[] getBytes() {
        return bytes;
    }

    /**
     * Sets the byte value.
     *
     * @param bytes bytes
     */
    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }
}
