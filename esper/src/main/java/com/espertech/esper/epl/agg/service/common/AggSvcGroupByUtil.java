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
package com.espertech.esper.epl.agg.service.common;

import com.espertech.esper.epl.agg.access.AggregationServicePassThru;
import com.espertech.esper.epl.agg.access.AggregationState;
import com.espertech.esper.epl.agg.aggregator.AggregationMethod;

public class AggSvcGroupByUtil {
    public static AggregationMethod[] newAggregators(AggregationMethodFactory[] prototypes) {
        AggregationMethod[] row = new AggregationMethod[prototypes.length];
        for (int i = 0; i < prototypes.length; i++) {
            row[i] = prototypes[i].make();
        }
        return row;
    }

    public static AggregationState[] newAccesses(int agentInstanceId, boolean isJoin, AggregationStateFactory[] accessAggSpecs, Object groupKey, AggregationServicePassThru passThru) {
        AggregationState[] row = new AggregationState[accessAggSpecs.length];
        int i = 0;
        for (AggregationStateFactory spec : accessAggSpecs) {
            row[i] = spec.createAccess(agentInstanceId, isJoin, groupKey, passThru);   // no group id assigned
            i++;
        }
        return row;
    }
}
