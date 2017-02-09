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

import java.io.UnsupportedEncodingException;

/**
 * Count-min sketch agent that handles String-type values and uses UTF-16 encoding
 * to transform strings to byte-array and back.
 */
public class CountMinSketchAgentStringUTF16 implements CountMinSketchAgent {

    public Class[] getAcceptableValueTypes() {
        return new Class[]{String.class};
    }

    public void add(CountMinSketchAgentContextAdd ctx) {
        String text = (String) ctx.getValue();
        if (text == null) {
            return;
        }
        byte[] bytes = toBytesUTF16(text);
        ctx.getState().add(bytes, 1);
    }

    public Long estimate(CountMinSketchAgentContextEstimate ctx) {
        String text = (String) ctx.getValue();
        if (text == null) {
            return null;
        }
        byte[] bytes = toBytesUTF16(text);
        return ctx.getState().frequency(bytes);
    }

    public Object fromBytes(CountMinSketchAgentContextFromBytes ctx) {
        try {
            return new String(ctx.getBytes(), "UTF-16");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] toBytesUTF16(String text) {
        try {
            return text.getBytes("UTF-16");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
