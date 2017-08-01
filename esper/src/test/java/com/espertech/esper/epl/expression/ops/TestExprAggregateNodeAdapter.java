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
package com.espertech.esper.epl.expression.ops;

import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.support.SupportStatementContextFactory;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNode;
import com.espertech.esper.supportunit.epl.SupportAggregationResultFuture;
import junit.framework.TestCase;

public abstract class TestExprAggregateNodeAdapter extends TestCase {
    protected ExprAggregateNode validatedNodeToTest;

    public void testEvaluate() throws Exception {
        SupportAggregationResultFuture future = new SupportAggregationResultFuture(new Object[]{10, 20});
        validatedNodeToTest.setAggregationResultFuture(future, 1);
        AgentInstanceContext agentInstanceContext = SupportStatementContextFactory.makeAgentInstanceContext();

        assertEquals(20, validatedNodeToTest.evaluate(null, false, agentInstanceContext));
    }
}

