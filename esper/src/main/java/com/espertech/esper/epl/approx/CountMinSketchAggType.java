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
package com.espertech.esper.epl.approx;

import java.util.Locale;

public enum CountMinSketchAggType {
    STATE("countMinSketch"),
    ADD("countMinSketchAdd"),
    FREQ("countMinSketchFrequency"),
    TOPK("countMinSketchTopk");

    private final String funcName;

    private CountMinSketchAggType(String funcName) {
        this.funcName = funcName;
    }

    public String getFuncName() {
        return funcName;
    }

    public static CountMinSketchAggType fromNameMayMatch(String name) {
        String nameLower = name.toLowerCase(Locale.ENGLISH);
        for (CountMinSketchAggType value : CountMinSketchAggType.values()) {
            if (value.funcName.toLowerCase(Locale.ENGLISH).equals(nameLower)) {
                return value;
            }
        }
        return null;
    }
}
