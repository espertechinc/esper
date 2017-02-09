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
package com.espertech.esper.epl.join.util;

import com.espertech.esper.epl.join.plan.QueryPlan;

public interface QueryPlanIndexHook {
    public void subquery(QueryPlanIndexDescSubquery subquery);

    public void infraOnExpr(QueryPlanIndexDescOnExpr onexpr);

    public void fireAndForget(QueryPlanIndexDescFAF queryPlanIndexDescFAF);

    public void join(QueryPlan join);

    public void historical(QueryPlanIndexDescHistorical historical);
}
