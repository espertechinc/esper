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
package com.espertech.esper.regressionlib.support.extend.aggmultifunc;

import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionState;
import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionStateFactory;
import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionStateFactoryContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;

import java.util.ArrayList;
import java.util.List;

public class SupportAggMFMultiRTSingleEventStateFactory implements AggregationMultiFunctionStateFactory {

    private static List<SupportAggMFMultiRTSingleEventState> stateContexts = new ArrayList<>();

    public static void reset() {
        stateContexts.clear();
    }

    public static void clear() {
        stateContexts.clear();
    }

    public static List<SupportAggMFMultiRTSingleEventState> getStateContexts() {
        return stateContexts;
    }

    private ExprEvaluator param;

    public AggregationMultiFunctionState newState(AggregationMultiFunctionStateFactoryContext ctx) {
        SupportAggMFMultiRTSingleEventState state = new SupportAggMFMultiRTSingleEventState();
        stateContexts.add(state);
        return state;
    }

    public void setParam(ExprEvaluator param) {
        this.param = param;
    }

    public ExprEvaluator getParam() {
        return param;
    }
}
