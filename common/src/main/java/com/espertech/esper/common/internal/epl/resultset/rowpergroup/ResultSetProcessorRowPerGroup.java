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
package com.espertech.esper.common.internal.epl.resultset.rowpergroup;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.epl.agg.core.AggregationRowRemovedCallback;
import com.espertech.esper.common.internal.epl.agg.core.AggregationService;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessor;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprProcessor;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public interface ResultSetProcessorRowPerGroup extends ResultSetProcessor, AggregationRowRemovedCallback {
    Object generateGroupKeySingle(EventBean[] eventsPerStream, boolean isNewData);

    boolean hasHavingClause();

    boolean evaluateHavingClause(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext);

    boolean isSelectRStream();

    AggregationService getAggregationService();

    ExprEvaluatorContext getAgentInstanceContext();

    SelectExprProcessor getSelectExprProcessor();

    EventBean generateOutputBatchedNoSortWMap(boolean join, Object mk, EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize);

    void generateOutputBatchedArrFromIterator(boolean join, Iterator<Map.Entry<Object, EventBean[]>> keysAndEvents, boolean isNewData, boolean isSynthesize, List<EventBean> resultEvents, List<Object> optSortKeys);
}
