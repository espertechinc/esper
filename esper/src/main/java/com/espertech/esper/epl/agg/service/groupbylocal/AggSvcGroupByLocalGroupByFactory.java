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
package com.espertech.esper.epl.agg.service.groupbylocal;

import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.agg.service.common.AggregationService;
import com.espertech.esper.epl.agg.service.common.AggregationServiceFactory;
import com.espertech.esper.epl.agg.util.AggregationLocalGroupByPlan;
import com.espertech.esper.epl.core.engineimport.EngineImportService;

public class AggSvcGroupByLocalGroupByFactory implements AggregationServiceFactory {

    protected final boolean join;
    protected final AggregationLocalGroupByPlan localGroupByPlan;

    public AggSvcGroupByLocalGroupByFactory(boolean join, AggregationLocalGroupByPlan localGroupByPlan) {
        this.join = join;
        this.localGroupByPlan = localGroupByPlan;
    }

    public AggregationService makeService(AgentInstanceContext agentInstanceContext, EngineImportService engineImportService, boolean isSubquery, Integer subqueryNumber) {
        return new AggSvcGroupByLocalGroupBy(join, localGroupByPlan);
    }
}
