/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.support.epl;

import com.espertech.esper.client.hook.AggregationFunctionFactory;
import com.espertech.esper.epl.agg.aggregator.AggregationMethod;
import com.espertech.esper.epl.agg.service.AggregationValidationContext;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SupportPluginAggregationMethodThreeFactory implements Serializable, AggregationFunctionFactory
{
    private static List<AggregationValidationContext> contexts = new ArrayList<AggregationValidationContext>();

    public static List<AggregationValidationContext> getContexts()
    {
        return contexts;
    }

    public void validate(AggregationValidationContext validationContext)
    {
        contexts.add(validationContext);
    }

    public Class getValueType()
    {
        return int.class;
    }

    public void setFunctionName(String functionName) {

    }

    public AggregationMethod newAggregator() {
        return new SupportPluginAggregationMethodThree();
    }
}
