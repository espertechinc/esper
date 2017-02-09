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
package com.espertech.esper.epl.lookup;

import java.util.Arrays;

public class LookupStrategyDesc {
    private final LookupStrategyType lookupStrategy;
    private final String[] expressionsTexts;

    public LookupStrategyDesc(LookupStrategyType lookupStrategy, String[] expressionsTexts) {
        this.lookupStrategy = lookupStrategy;
        this.expressionsTexts = expressionsTexts;
    }

    public LookupStrategyType getLookupStrategy() {
        return lookupStrategy;
    }

    public String[] getExpressionsTexts() {
        return expressionsTexts;
    }

    public String toString() {
        return "LookupStrategyDesc{" +
                "lookupStrategy=" + lookupStrategy +
                ", expressionsTexts=" + (expressionsTexts == null ? null : Arrays.asList(expressionsTexts)) +
                '}';
    }
}
