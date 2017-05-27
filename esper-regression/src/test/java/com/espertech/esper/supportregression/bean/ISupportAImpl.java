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

public class ISupportAImpl implements ISupportA, Serializable {
    private String valueA;
    private String valueBaseAB;

    public ISupportAImpl(String valueA, String valueBaseAB) {
        this.valueA = valueA;
        this.valueBaseAB = valueBaseAB;
    }

    public String getA() {
        return valueA;
    }

    public String getBaseAB() {
        return valueBaseAB;
    }
}
