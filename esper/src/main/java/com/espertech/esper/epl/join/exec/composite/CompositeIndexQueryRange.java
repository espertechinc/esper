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
package com.espertech.esper.epl.join.exec.composite;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.join.plan.QueryGraphRangeEnum;
import com.espertech.esper.epl.join.plan.QueryGraphValueEntryRange;
import com.espertech.esper.epl.join.plan.QueryGraphValueEntryRangeIn;
import com.espertech.esper.epl.join.plan.QueryGraphValueEntryRangeRelOp;
import com.espertech.esper.epl.lookup.SubordPropRangeKey;

import java.util.*;

public class CompositeIndexQueryRange implements CompositeIndexQuery {

    private final CompositeAccessStrategy strategy;
    private CompositeIndexQuery next;

    public CompositeIndexQueryRange(boolean isNWOnTrigger, int lookupStream, int numStreams, SubordPropRangeKey subqRangeKey, Class coercionType, List<String> expressionTexts) {

        QueryGraphValueEntryRange rangeProp = subqRangeKey.getRangeInfo();

        if (rangeProp.getType().isRange()) {
            QueryGraphValueEntryRangeIn rangeIn = (QueryGraphValueEntryRangeIn) rangeProp;
            ExprEvaluator start = rangeIn.getExprStart().getForge().getExprEvaluator();
            expressionTexts.add(ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(rangeIn.getExprStart()));
            boolean includeStart = rangeProp.getType().isIncludeStart();

            ExprEvaluator end = rangeIn.getExprEnd().getForge().getExprEvaluator();
            expressionTexts.add(ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(rangeIn.getExprEnd()));
            boolean includeEnd = rangeProp.getType().isIncludeEnd();

            if (!rangeProp.getType().isRangeInverted()) {
                strategy = new CompositeAccessStrategyRangeNormal(isNWOnTrigger, lookupStream, numStreams, start, includeStart, end, includeEnd, coercionType, ((QueryGraphValueEntryRangeIn) rangeProp).isAllowRangeReversal());
            } else {
                strategy = new CompositeAccessStrategyRangeInverted(isNWOnTrigger, lookupStream, numStreams, start, includeStart, end, includeEnd, coercionType);
            }
        } else {
            QueryGraphValueEntryRangeRelOp relOp = (QueryGraphValueEntryRangeRelOp) rangeProp;
            ExprEvaluator key = relOp.getExpression().getForge().getExprEvaluator();
            expressionTexts.add(ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(relOp.getExpression()));
            if (rangeProp.getType() == QueryGraphRangeEnum.GREATER_OR_EQUAL) {
                strategy = new CompositeAccessStrategyGE(isNWOnTrigger, lookupStream, numStreams, key, coercionType);
            } else if (rangeProp.getType() == QueryGraphRangeEnum.GREATER) {
                strategy = new CompositeAccessStrategyGT(isNWOnTrigger, lookupStream, numStreams, key, coercionType);
            } else if (rangeProp.getType() == QueryGraphRangeEnum.LESS_OR_EQUAL) {
                strategy = new CompositeAccessStrategyLE(isNWOnTrigger, lookupStream, numStreams, key, coercionType);
            } else if (rangeProp.getType() == QueryGraphRangeEnum.LESS) {
                strategy = new CompositeAccessStrategyLT(isNWOnTrigger, lookupStream, numStreams, key, coercionType);
            } else {
                throw new IllegalArgumentException("Comparison operator " + rangeProp.getType() + " not supported");
            }
        }
    }

    public void add(EventBean theEvent, Map parent, Set<EventBean> result, CompositeIndexQueryResultPostProcessor postProcessor) {
        strategy.lookup(theEvent, parent, result, next, null, null, postProcessor);
    }

    public void add(EventBean[] eventsPerStream, Map parent, Collection<EventBean> result, CompositeIndexQueryResultPostProcessor postProcessor) {
        strategy.lookup(eventsPerStream, parent, result, next, null, null, postProcessor);
    }

    public Set<EventBean> get(EventBean theEvent, Map parent, ExprEvaluatorContext context, CompositeIndexQueryResultPostProcessor postProcessor) {
        return strategy.lookup(theEvent, parent, null, next, context, null, postProcessor);
    }

    public Collection<EventBean> get(EventBean[] eventsPerStream, Map parent, ExprEvaluatorContext context, CompositeIndexQueryResultPostProcessor postProcessor) {
        return strategy.lookup(eventsPerStream, parent, null, next, context, null, postProcessor);
    }

    public Set<EventBean> getCollectKeys(EventBean theEvent, Map parent, ExprEvaluatorContext context, ArrayList<Object> keys, CompositeIndexQueryResultPostProcessor postProcessor) {
        return strategy.lookup(theEvent, parent, null, next, context, keys, postProcessor);
    }

    public Collection<EventBean> getCollectKeys(EventBean[] eventsPerStream, Map parent, ExprEvaluatorContext context, ArrayList<Object> keys, CompositeIndexQueryResultPostProcessor postProcessor) {
        return strategy.lookup(eventsPerStream, parent, null, next, context, keys, postProcessor);
    }

    protected static Set<EventBean> handle(EventBean theEvent, SortedMap sortedMapOne, SortedMap sortedMapTwo, Set<EventBean> result, CompositeIndexQuery next, CompositeIndexQueryResultPostProcessor postProcessor) {
        if (next == null) {
            if (result == null) {
                result = new HashSet<EventBean>();
            }
            addResults(sortedMapOne, sortedMapTwo, result, postProcessor);
            return result;
        } else {
            if (result == null) {
                result = new HashSet<EventBean>();
            }
            Map<Object, Map> map = (Map<Object, Map>) sortedMapOne;
            for (Map.Entry<Object, Map> entry : map.entrySet()) {
                next.add(theEvent, entry.getValue(), result, postProcessor);
            }
            if (sortedMapTwo != null) {
                map = (Map<Object, Map>) sortedMapTwo;
                for (Map.Entry<Object, Map> entry : map.entrySet()) {
                    next.add(theEvent, entry.getValue(), result, postProcessor);
                }
            }
            return result;
        }
    }

    protected static Collection<EventBean> handle(EventBean[] eventsPerStream, SortedMap sortedMapOne, SortedMap sortedMapTwo, Collection<EventBean> result, CompositeIndexQuery next, CompositeIndexQueryResultPostProcessor postProcessor) {
        if (next == null) {
            if (result == null) {
                result = new HashSet<EventBean>();
            }
            addResults(sortedMapOne, sortedMapTwo, result, postProcessor);
            return result;
        } else {
            if (result == null) {
                result = new HashSet<EventBean>();
            }
            Map<Object, Map> map = (Map<Object, Map>) sortedMapOne;
            for (Map.Entry<Object, Map> entry : map.entrySet()) {
                next.add(eventsPerStream, entry.getValue(), result, postProcessor);
            }
            if (sortedMapTwo != null) {
                map = (Map<Object, Map>) sortedMapTwo;
                for (Map.Entry<Object, Map> entry : map.entrySet()) {
                    next.add(eventsPerStream, entry.getValue(), result, postProcessor);
                }
            }
            return result;
        }
    }

    private static void addResults(SortedMap sortedMapOne, SortedMap sortedMapTwo, Collection<EventBean> result, CompositeIndexQueryResultPostProcessor postProcessor) {
        addResults(sortedMapOne, result, postProcessor);
        if (sortedMapTwo != null) {
            addResults(sortedMapTwo, result, postProcessor);
        }
    }

    private static void addResults(SortedMap sortedMapOne, Collection<EventBean> result, CompositeIndexQueryResultPostProcessor postProcessor) {
        Map<Object, Set<EventBean>> map = (Map<Object, Set<EventBean>>) sortedMapOne;

        if (postProcessor != null) {
            for (Map.Entry<Object, Set<EventBean>> entry : map.entrySet()) {
                postProcessor.add(entry.getValue(), result);
            }
        } else {
            for (Map.Entry<Object, Set<EventBean>> entry : map.entrySet()) {
                result.addAll(entry.getValue());
            }
        }
    }

    public void setNext(CompositeIndexQuery next) {
        this.next = next;
    }
}
