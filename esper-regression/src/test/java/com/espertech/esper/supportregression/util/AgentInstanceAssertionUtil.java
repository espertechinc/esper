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
package com.espertech.esper.supportregression.util;

import com.espertech.esper.core.context.stmt.StatementAIResourceRegistry;
import com.espertech.esper.core.service.StatementContext;
import org.junit.Assert;

public class AgentInstanceAssertionUtil {

    public static void assertInstanceCounts(StatementContext context, int numAggregations) {
        assertInstanceCounts(context, numAggregations, 0, 0, 0);
    }

    public static void assertInstanceCounts(StatementContext context, int numAggregations, int numSubselect, int numPrev, int numPrior) {
        StatementAIResourceRegistry registry = context.getStatementAgentInstanceRegistry();
        Assert.assertEquals(numAggregations, registry.getAgentInstanceAggregationService().getInstanceCount());
        Assert.assertEquals(numSubselect, registry.getAgentInstanceExprService().getSubselectAgentInstanceCount());
        Assert.assertEquals(numPrev, registry.getAgentInstanceExprService().getPreviousAgentInstanceCount());
        Assert.assertEquals(numPrior, registry.getAgentInstanceExprService().getPriorAgentInstanceCount());
    }
}
