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
package com.espertech.esper.supportregression.epl;

import com.espertech.esper.collection.Pair;
import com.espertech.esper.epl.agg.util.AggregationGroupByLocalGroupDesc;
import com.espertech.esper.epl.agg.util.AggregationLocalGroupByPlanForge;
import com.espertech.esper.epl.agg.util.AggregationLocalLevelHook;

public class SupportAggLevelPlanHook implements AggregationLocalLevelHook {

    private static Pair<AggregationGroupByLocalGroupDesc, AggregationLocalGroupByPlanForge> desc;

    public void planned(AggregationGroupByLocalGroupDesc localGroupDesc, AggregationLocalGroupByPlanForge localGroupByPlan) {
        desc = new Pair<>(localGroupDesc, localGroupByPlan);
    }

    public static Pair<AggregationGroupByLocalGroupDesc, AggregationLocalGroupByPlanForge> getAndReset() {
        Pair<AggregationGroupByLocalGroupDesc, AggregationLocalGroupByPlanForge> tmp = desc;
        desc = null;
        return tmp;
    }
}
