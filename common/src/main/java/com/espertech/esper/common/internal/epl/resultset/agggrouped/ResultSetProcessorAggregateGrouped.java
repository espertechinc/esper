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
package com.espertech.esper.common.internal.epl.resultset.agggrouped;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.collection.MultiKeyArrayOfKeys;
import com.espertech.esper.common.internal.epl.agg.core.AggregationRowRemovedCallback;
import com.espertech.esper.common.internal.epl.agg.core.AggregationService;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessor;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprProcessor;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ResultSetProcessorAggregateGrouped extends ResultSetProcessor, AggregationRowRemovedCallback {
    boolean hasHavingClause();

    boolean evaluateHavingClause(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext);

    SelectExprProcessor getSelectExprProcessor();

    AggregationService getAggregationService();

    ExprEvaluatorContext getAgentInstanceContext();

    boolean isSelectRStream();

    Object generateGroupKeySingle(EventBean[] eventsPerStream, boolean isNewData);

    Object[] generateGroupKeyArrayJoin(Set<MultiKeyArrayOfKeys<EventBean>> newData, boolean isNewData);

    Object[] generateGroupKeyArrayView(EventBean[] oldData, boolean isNewData);

    EventBean generateOutputBatchedSingle(Object key, EventBean[] event, boolean isNewData, boolean isSynthesize);

    void generateOutputBatchedViewUnkeyed(EventBean[] outputEvents, Object[] groupByKeys, boolean isNewData, boolean isSynthesize, Collection<EventBean> resultEvents, List<Object> optSortKeys, EventBean[] eventsPerStream);

    void generateOutputBatchedJoinUnkeyed(Set<MultiKeyArrayOfKeys<EventBean>> outputEvents, Object[] groupByKeys, boolean isNewData, boolean isSynthesize, Collection<EventBean> resultEvents, List<Object> optSortKeys);

    void generateOutputBatchedViewPerKey(EventBean[] oldData, Object[] oldDataMultiKey, boolean isNewData, boolean isGenerateSynthetic, Map<Object, EventBean> outputLastUnordGroupOld, Map<Object, Object> optSortKeys, EventBean[] eventsPerStream);

    void generateOutputBatchedJoinPerKey(Set<MultiKeyArrayOfKeys<EventBean>> outputEvents, Object[] groupByKeys, boolean isNewData, boolean isSynthesize, Map<Object, EventBean> resultEvents, Map<Object, Object> optSortKeys);
}
