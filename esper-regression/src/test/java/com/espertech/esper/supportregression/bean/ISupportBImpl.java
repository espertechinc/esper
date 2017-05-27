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
package com.espertech.esper.supportregression.bean;

import java.io.Serializable;

public class ISupportBImpl implements ISupportB, Serializable {
    private String valueB;
    private String valueBaseAB;

    public ISupportBImpl(String valueB, String valueBaseAB) {
        this.valueB = valueB;
        this.valueBaseAB = valueBaseAB;
    }

    public String getB() {
        return valueB;
    }

    public String getBaseAB() {
        return valueBaseAB;
    }

    public String toString() {
        return "ISupportBImpl{" +
                "valueB='" + valueB + '\'' +
                ", valueBaseAB='" + valueBaseAB + '\'' +
                '}';
    }
}
