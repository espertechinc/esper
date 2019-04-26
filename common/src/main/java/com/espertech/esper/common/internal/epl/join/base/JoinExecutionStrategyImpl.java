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
package com.espertech.esper.common.internal.epl.join.base;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.collection.MultiKeyArrayOfKeys;
import com.espertech.esper.common.internal.collection.UniformPair;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationCommon;

import java.util.Set;

import static com.espertech.esper.common.internal.epl.join.base.JoinSetComposerUtil.filter;

/**
 * Join execution strategy based on a 3-step getSelectListEvents of composing a join set, filtering the join set and
 * indicating.
 */
public class JoinExecutionStrategyImpl implements JoinExecutionStrategy {
    private final JoinSetComposer composer;
    private final ExprEvaluator optionalFilter;
    private final JoinSetProcessor indicator;
    private final ExprEvaluatorContext staticExprEvaluatorContext;

    /**
     * Ctor.
     *
     * @param composer                   - determines join tuple set
     * @param optionalFilter             - for filtering among tuples
     * @param indicator                  - for presenting the info to a view
     * @param staticExprEvaluatorContext expression evaluation context for static evaluation (not for runtime eval)
     */
    public JoinExecutionStrategyImpl(JoinSetComposer composer, ExprEvaluator optionalFilter, JoinSetProcessor indicator,
                                     ExprEvaluatorContext staticExprEvaluatorContext) {
        this.composer = composer;
        this.optionalFilter = optionalFilter;
        this.indicator = indicator;
        this.staticExprEvaluatorContext = staticExprEvaluatorContext;
    }

    public void join(EventBean[][] newDataPerStream, EventBean[][] oldDataPerStream) {
        InstrumentationCommon instrumentationCommon = staticExprEvaluatorContext.getInstrumentationProvider();
        instrumentationCommon.qJoinExecStrategy();

        UniformPair<Set<MultiKeyArrayOfKeys<EventBean>>> joinSet = composer.join(newDataPerStream, oldDataPerStream, staticExprEvaluatorContext);

        instrumentationCommon.aJoinExecStrategy(joinSet);

        if (optionalFilter != null) {
            instrumentationCommon.qJoinExecFilter();
            processFilter(joinSet.getFirst(), joinSet.getSecond(), staticExprEvaluatorContext);
            instrumentationCommon.aJoinExecFilter(joinSet.getFirst(), joinSet.getSecond());
        }

        if ((!joinSet.getFirst().isEmpty()) || (!joinSet.getSecond().isEmpty())) {
            instrumentationCommon.qJoinExecProcess(joinSet);
            indicator.process(joinSet.getFirst(), joinSet.getSecond(), staticExprEvaluatorContext);
            instrumentationCommon.aJoinExecProcess();
        }
    }

    public Set<MultiKeyArrayOfKeys<EventBean>> staticJoin() {
        Set<MultiKeyArrayOfKeys<EventBean>> joinSet = composer.staticJoin();
        if (optionalFilter != null) {
            processFilter(joinSet, null, staticExprEvaluatorContext);
        }
        return joinSet;
    }

    private void processFilter(Set<MultiKeyArrayOfKeys<EventBean>> newEvents, Set<MultiKeyArrayOfKeys<EventBean>> oldEvents, ExprEvaluatorContext exprEvaluatorContext) {
        filter(optionalFilter, newEvents, true, exprEvaluatorContext);
        if (oldEvents != null) {
            filter(optionalFilter, oldEvents, false, exprEvaluatorContext);
        }
    }
}
