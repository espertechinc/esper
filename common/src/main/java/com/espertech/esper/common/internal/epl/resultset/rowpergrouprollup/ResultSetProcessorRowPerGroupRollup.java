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
package com.espertech.esper.common.internal.epl.resultset.rowpergrouprollup;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.epl.agg.core.*;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessor;

import java.util.List;
import java.util.Map;

public interface ResultSetProcessorRowPerGroupRollup extends ResultSetProcessor, AggregationRowRemovedCallback {
    AggregationService getAggregationService();

    ExprEvaluatorContext getAgentInstanceContext();

    boolean isSelectRStream();

    AggregationGroupByRollupDesc getGroupByRollupDesc();

    Object generateGroupKeySingle(EventBean[] eventsPerStream, boolean isNewData);

    void generateOutputBatchedMapUnsorted(boolean join, Object mk, AggregationGroupByRollupLevel level, EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, Map<Object, EventBean> resultEvents);

    void generateOutputBatched(Object mk, AggregationGroupByRollupLevel level, EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, List<EventBean> resultEvents, List<Object> optSortKeys);
}

