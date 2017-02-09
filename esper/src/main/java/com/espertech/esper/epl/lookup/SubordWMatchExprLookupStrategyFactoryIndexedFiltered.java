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
package com.espertech.esper.epl.lookup;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.join.table.EventTable;
import com.espertech.esper.epl.virtualdw.VirtualDWView;

public class SubordWMatchExprLookupStrategyFactoryIndexedFiltered implements SubordWMatchExprLookupStrategyFactory {
    private final ExprEvaluator exprEvaluator;
    private final SubordTableLookupStrategyFactory lookupStrategyFactory;

    public SubordWMatchExprLookupStrategyFactoryIndexedFiltered(ExprEvaluator exprEvaluator, SubordTableLookupStrategyFactory lookupStrategyFactory) {
        this.exprEvaluator = exprEvaluator;
        this.lookupStrategyFactory = lookupStrategyFactory;
    }

    public SubordWMatchExprLookupStrategy realize(EventTable[] indexes, AgentInstanceContext agentInstanceContext, Iterable<EventBean> scanIterable, VirtualDWView virtualDataWindow) {
        SubordTableLookupStrategy strategy = lookupStrategyFactory.makeStrategy(indexes, virtualDataWindow);
        return new SubordWMatchExprLookupStrategyIndexedFiltered(exprEvaluator, strategy);
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName() + " " + " strategy " + lookupStrategyFactory.toQueryPlan();
    }

    public SubordTableLookupStrategyFactory getOptionalInnerStrategy() {
        return lookupStrategyFactory;
    }
}
