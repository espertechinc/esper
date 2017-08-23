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
package com.espertech.esper.supportunit.epl;

import com.espertech.esper.epl.agg.aggregator.AggregationMethod;
import com.espertech.esper.epl.core.engineimport.EngineImportService;

public class SupportAggregator implements AggregationMethod {
    private int sum;

    public void clear() {

    }

    public void enter(Object value) {
        if (value != null) {
            sum += (Integer) value;
        }
    }

    public void leave(Object value) {
        if (value != null) {
            sum -= (Integer) value;
        }
    }

    public Object getValue() {
        return sum;
    }

    public AggregationMethod newAggregator(EngineImportService engineImportService) {
        return new SupportAggregator();
    }

    public String getFunctionName() {
        return "supportagg";
    }
}
