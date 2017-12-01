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
import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.join.plan.InKeywordTableLookupUtil;
import com.espertech.esper.epl.join.table.PropertyIndexedEventTableSingle;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.util.Collection;
import java.util.Set;

/**
 * Index lookup strategy for subqueries for in-keyword single-index sided.
 */
public class SubordInKeywordSingleTableLookupStrategyNW implements SubordTableLookupStrategy {
    /**
     * Index to look up in.
     */
    protected final PropertyIndexedEventTableSingle index;

    protected final ExprEvaluator[] evaluators;

    protected final LookupStrategyDesc strategyDesc;

    public SubordInKeywordSingleTableLookupStrategyNW(ExprEvaluator[] evaluators, PropertyIndexedEventTableSingle index, LookupStrategyDesc strategyDesc) {
        this.evaluators = evaluators;
        this.index = index;
        this.strategyDesc = strategyDesc;
    }

    /**
     * Returns index to look up in.
     *
     * @return index to use
     */
    public PropertyIndexedEventTableSingle getIndex() {
        return index;
    }

    public Collection<EventBean> lookup(EventBean[] eventsPerStream, ExprEvaluatorContext context) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qIndexSubordLookup(this, index, null);
        }

        Set<EventBean> result = InKeywordTableLookupUtil.singleIndexLookup(evaluators, eventsPerStream, context, index);

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aIndexSubordLookup(result, null);
        }
        return result;
    }

    public LookupStrategyDesc getStrategyDesc() {
        return strategyDesc;
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName() + " evaluators " + ExprNodeUtilityCore.printEvaluators(evaluators);
    }
}
