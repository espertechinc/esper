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
package com.espertech.esper.supportregression.multithread;

import com.espertech.esper.client.hook.*;
import com.espertech.esper.epl.agg.aggregator.AggregationMethod;
import com.espertech.esper.epl.agg.service.common.AggregationValidationContext;

import java.util.List;

public class MyIntListAggregationFactory implements AggregationFunctionFactory {

    public void validate(AggregationValidationContext validationContext) {
    }

    @Override
    public Class getValueType() {
        return List.class;
    }

    public void setFunctionName(String functionName) {

    }

    public AggregationMethod newAggregator() {
        return new MyIntListAggregation();
    }
}
