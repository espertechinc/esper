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
package com.espertech.esper.regressionlib.support.extend.aggfunc;

import com.espertech.esper.common.client.hook.aggfunc.AggregationFunction;
import com.espertech.esper.common.client.hook.aggfunc.AggregationFunctionFactory;
import com.espertech.esper.common.client.hook.aggfunc.AggregationFunctionFactoryContext;

public class SupportSupportBeanAggregationFunctionFactory implements AggregationFunctionFactory {
    private static int instanceCount;

    public static void setInstanceCount(int instanceCount) {
        SupportSupportBeanAggregationFunctionFactory.instanceCount = instanceCount;
    }

    public static int getInstanceCount() {
        return instanceCount;
    }

    public static void incInstanceCount() {
        instanceCount++;
    }

    public AggregationFunction newAggregator(AggregationFunctionFactoryContext ctx) {
        instanceCount++;
        return new SupportSupportBeanAggregationFunction();
    }
}
