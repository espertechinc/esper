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
package com.espertech.esper.regressionlib.support.filter;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.compile.stage2.FilterSpecPlanForge;
import com.espertech.esper.common.internal.compile.stage2.FilterSpecPlanPathForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.filterspec.FilterSpecParamForge;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class SupportFilterPlanEntry {
    private final EventType eventType;
    private final FilterSpecPlanForge plan;
    private final List<ExprNode> planNodes;

    public SupportFilterPlanEntry(EventType eventType, FilterSpecPlanForge forges, List<ExprNode> planNodes) {
        this.eventType = eventType;
        this.plan = forges;
        this.planNodes = planNodes;
    }

    public EventType getEventType() {
        return eventType;
    }

    public FilterSpecPlanForge getPlan() {
        return plan;
    }

    public List<ExprNode> getPlanNodes() {
        return planNodes;
    }

    public FilterSpecParamForge getAssertSingle(String typeName) {
        assertEquals(typeName, eventType.getName());
        assertEquals(1, plan.getPaths().length);
        FilterSpecPlanPathForge path = plan.getPaths()[0];
        assertEquals(1, path.getTriplets().length);
        return path.getTriplets()[0].getParam();
    }
}
