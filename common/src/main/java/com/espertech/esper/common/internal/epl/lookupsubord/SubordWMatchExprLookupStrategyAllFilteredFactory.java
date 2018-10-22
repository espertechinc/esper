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
package com.espertech.esper.common.internal.epl.lookupsubord;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.index.base.EventTable;
import com.espertech.esper.common.internal.epl.lookup.SubordTableLookupStrategyFactory;
import com.espertech.esper.common.internal.epl.lookupplansubord.SubordWMatchExprLookupStrategy;
import com.espertech.esper.common.internal.epl.lookupplansubord.SubordWMatchExprLookupStrategyFactory;
import com.espertech.esper.common.internal.epl.virtualdw.VirtualDWView;

public class SubordWMatchExprLookupStrategyAllFilteredFactory implements SubordWMatchExprLookupStrategyFactory {
    private final ExprEvaluator exprEvaluator;

    public SubordWMatchExprLookupStrategyAllFilteredFactory(ExprEvaluator exprEvaluator) {
        this.exprEvaluator = exprEvaluator;
    }

    public SubordWMatchExprLookupStrategy realize(EventTable[] indexes, AgentInstanceContext agentInstanceContext, Iterable<EventBean> scanIterable, VirtualDWView virtualDataWindow) {
        return new SubordWMatchExprLookupStrategyAllFiltered(exprEvaluator, scanIterable);
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName();
    }

    public SubordTableLookupStrategyFactory getOptionalInnerStrategy() {
        return null;
    }
}
