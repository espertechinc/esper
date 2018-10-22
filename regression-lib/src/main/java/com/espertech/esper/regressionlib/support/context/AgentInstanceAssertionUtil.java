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
package com.espertech.esper.regressionlib.support.context;


import com.espertech.esper.common.internal.context.airegistry.StatementAIResourceRegistry;
import com.espertech.esper.common.internal.context.util.StatementContext;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.runtime.internal.kernel.statement.EPStatementSPI;

import static org.junit.Assert.*;

public class AgentInstanceAssertionUtil {

    public static void assertInstanceCounts(RegressionEnvironment env, String statementName, int numAggregations) {
        EPStatementSPI stmt = (EPStatementSPI) env.statement(statementName);
        if (stmt == null) {
            fail("Statement not found '" + statementName + "'");
        }
        assertInstanceCounts(env, stmt.getStatementContext(), numAggregations, null, null, null);
    }

    public static void assertInstanceCounts(RegressionEnvironment env, String statementName, Integer numAggregations, Integer numSubselect, Integer numPrev, Integer numPrior) {
        EPStatementSPI stmt = (EPStatementSPI) env.statement(statementName);
        if (stmt == null) {
            fail("Statement not found '" + statementName + "'");
        }
        assertInstanceCounts(env, stmt.getStatementContext(), numAggregations, numSubselect, numPrev, numPrior);
    }

    private static void assertInstanceCounts(RegressionEnvironment env, StatementContext context, Integer numAggregations, Integer numSubselect, Integer numPrev, Integer numPrior) {
        if (env.isHA_Releasing()) {
            return;
        }

        StatementAIResourceRegistry registry = context.getStatementAIResourceRegistry();
        if (numAggregations != null) {
            assertEquals((int) numAggregations, registry.getAgentInstanceAggregationService().getInstanceCount());
        } else {
            assertNull(registry.getAgentInstanceAggregationService());
        }

        if (numSubselect != null) {
            assertEquals((int) numSubselect, registry.getAgentInstanceSubselects().get(0).getLookupStrategies().getInstanceCount());
        } else {
            assertNull(registry.getAgentInstanceSubselects());
        }

        if (numPrev != null) {
            assertEquals((int) numPrev, registry.getAgentInstancePreviousGetterStrategies()[0].getInstanceCount());
        } else {
            assertNull(registry.getAgentInstancePreviousGetterStrategies());
        }

        if (numPrior != null) {
            assertEquals((int) numPrior, registry.getAgentInstancePriorEvalStrategies()[0].getInstanceCount());
        } else {
            assertNull(registry.getAgentInstancePriorEvalStrategies());
        }
    }
}
