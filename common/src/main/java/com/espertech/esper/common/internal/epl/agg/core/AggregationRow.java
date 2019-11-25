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
package com.espertech.esper.common.internal.epl.agg.core;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;

import java.util.Collection;

public interface AggregationRow {
    void applyEnter(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext);

    void applyLeave(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext);

    void enterAgg(int column, Object value);

    void leaveAgg(int column, Object value);

    void enterAccess(int column, EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext);

    void leaveAccess(int column, EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext);

    Object getAccessState(int column);

    void clear();

    Object getValue(int column, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext);

    Collection<EventBean> getCollectionOfEvents(int column, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context);

    EventBean getEventBean(int column, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context);

    Collection<Object> getCollectionScalar(int column, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context);

    void increaseRefcount();

    void decreaseRefcount();

    long getRefcount();

    long getLastUpdateTime();

    void setLastUpdateTime(long currentTime);

    void reset(int column);
}
