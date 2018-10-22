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
package com.espertech.esper.regressionlib.support.util;

import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.epl.agg.groupbylocal.AggregationGroupByLocalGroupDesc;
import com.espertech.esper.common.internal.epl.agg.groupbylocal.AggregationLocalGroupByPlanForge;
import com.espertech.esper.common.internal.epl.agg.groupbylocal.AggregationLocalLevelHook;

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
