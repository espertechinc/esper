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

public class ISupportAImplSuperGImplPlus extends ISupportAImplSuperG implements ISupportB, ISupportC, Serializable {
    String valueG;
    String valueA;
    String valueBaseAB;
    String valueB;
    String valueC;

    public ISupportAImplSuperGImplPlus() {
    }

    public ISupportAImplSuperGImplPlus(String valueG, String valueA, String valueBaseAB, String valueB, String valueC) {
        this.valueG = valueG;
        this.valueA = valueA;
        this.valueBaseAB = valueBaseAB;
        this.valueB = valueB;
        this.valueC = valueC;
    }

    public String getG() {
        return valueG;
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
