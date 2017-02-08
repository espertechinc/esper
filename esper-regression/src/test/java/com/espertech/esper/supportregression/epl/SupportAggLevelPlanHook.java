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
import com.espertech.esper.epl.agg.util.AggregationLocalGroupByPlan;
import com.espertech.esper.epl.agg.util.AggregationGroupByLocalGroupDesc;
import com.espertech.esper.epl.agg.util.AggregationLocalLevelHook;

public class SupportAggLevelPlanHook implements AggregationLocalLevelHook {

    private static Pair<AggregationGroupByLocalGroupDesc,AggregationLocalGroupByPlan> desc;

    public void planned(AggregationGroupByLocalGroupDesc localGroupDesc, AggregationLocalGroupByPlan localGroupByPlan) {
        desc = new Pair<AggregationGroupByLocalGroupDesc,AggregationLocalGroupByPlan>(localGroupDesc, localGroupByPlan);
    }

    public static Pair<AggregationGroupByLocalGroupDesc,AggregationLocalGroupByPlan> getAndReset() {
        Pair<AggregationGroupByLocalGroupDesc,AggregationLocalGroupByPlan> tmp = desc;
        desc = null;
        return tmp;
    }
}
