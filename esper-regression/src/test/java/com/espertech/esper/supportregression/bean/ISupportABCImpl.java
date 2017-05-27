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

public class ISupportABCImpl implements ISupportA, ISupportB, ISupportC {
    private String valueA;
    private String valueB;
    private String valueBaseAB;
    private String valueC;

    public ISupportABCImpl(String valueA, String valueB, String valueBaseAB, String valueC) {
        this.valueA = valueA;
        this.valueB = valueB;
        this.valueBaseAB = valueBaseAB;
        this.valueC = valueC;
    }

    public String getA() {
        return valueA;
    }

    public String getBaseAB() {
        return valueBaseAB;
    }

    public String getB() {
        return valueB;
    }

    public String getC() {
        return valueC;
    }
}
