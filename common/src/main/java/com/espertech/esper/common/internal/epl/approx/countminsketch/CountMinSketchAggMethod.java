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
package com.espertech.esper.common.internal.epl.approx.countminsketch;

import java.util.Locale;

public enum CountMinSketchAggMethod {
    FREQ("countMinSketchFrequency"),
    TOPK("countMinSketchTopk");

    private final String funcName;

    private CountMinSketchAggMethod(String funcName) {
        this.funcName = funcName;
    }

    public String getMethodName() {
        return funcName;
    }

    public static CountMinSketchAggMethod fromNameMayMatch(String name) {
        String nameLower = name.toLowerCase(Locale.ENGLISH);
        for (CountMinSketchAggMethod value : CountMinSketchAggMethod.values()) {
            if (value.funcName.toLowerCase(Locale.ENGLISH).equals(nameLower)) {
                return value;
            }
        }
        return null;
    }
}
