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

import com.espertech.esper.client.hook.AggregationFunctionFactory;
import com.espertech.esper.epl.agg.aggregator.AggregationMethod;
import com.espertech.esper.epl.agg.service.common.AggregationValidationContext;

public class SupportPluginAggregationMethodOneFactory implements AggregationFunctionFactory {
    public void setFunctionName(String functionName) {
    }

    public void validate(AggregationValidationContext validationContext) {
    }

    public AggregationMethod newAggregator() {
        return new SupportPluginAggregationMethodOne();
    }

    public Class getValueType() {
        return null;
    }
}
