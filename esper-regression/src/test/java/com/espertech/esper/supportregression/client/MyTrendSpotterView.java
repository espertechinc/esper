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
package com.espertech.esper.supportregression.client;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.collection.SingleEventIterator;
import com.espertech.esper.core.context.util.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.view.View;
import com.espertech.esper.view.ViewSupport;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MyTrendSpotterView extends ViewSupport {
    private static final String PROPERTY_NAME = "trendcount";

    private final AgentInstanceViewFactoryChainContext agentInstanceContext;
    private final EventType eventType;
    private final ExprNode expression;
    private final EventBean[] eventsPerStream = new EventBean[1];

    private Long trendcount;
    private Double lastDataPoint;

    // The remove stream must post the same object event reference
    private EventBean lastInsertStreamEvent;

    /**
     * Constructor requires the name of the field to use in the parent view to compute a trend.
     *
     * @param expression           is the name of the field within the parent view to use to get numeric data points for this view
     * @param agentInstanceContext contains required view services
     */
    public MyTrendSpotterView(AgentInstanceViewFactoryChainContext agentInstanceContext, ExprNode expression) {
        this.agentInstanceContext = agentInstanceContext;
        this.expression = expression;
        eventType = createEventType(agentInstanceContext.getStatementContext());
    }

    /**
     * Returns expression to report statistics on.
     *
     * @return expression providing values
     */
    public final ExprNode getExpression() {
        return expression;
    }

    public final void update(EventBean[] newData, EventBean[] oldData) {
        // The remove stream most post the same exact object references of events that were posted as the insert stream
        EventBean[] removeStreamToPost;
        if (lastInsertStreamEvent != null) {
            removeStreamToPost = new EventBean[]{lastInsertStreamEvent};
        } else {
            removeStreamToPost = new EventBean[]{populateMap(null)};
        }

        // add data points
        if (newData != null) {
            for (EventBean aNewData : newData) {
                eventsPerStream[0] = aNewData;
                double dataPoint = ((Number) expression.getForge().getExprEvaluator().evaluate(eventsPerStream, true, null)).doubleValue();

                if (lastDataPoint == null) {
                    trendcount = 1L;
                } else if (lastDataPoint < dataPoint) {
                    trendcount++;
                } else if (lastDataPoint > dataPoint) {
                    trendcount = 0L;
                }
                lastDataPoint = dataPoint;
            }
        }

        if (this.hasViews()) {
            EventBean newDataPost = populateMap(trendcount);
            lastInsertStreamEvent = newDataPost;
            updateChildren(new EventBean[]{newDataPost}, removeStreamToPost);
        }
    }

    public final EventType getEventType() {
        return eventType;
    }

    public final Iterator<EventBean> iterator() {
        EventBean theEvent = populateMap(trendcount);
        return new SingleEventIterator(theEvent);
    }

    public final String toString() {
        return this.getClass().getName() + " expression=" + expression;
    }

    private EventBean populateMap(Long trendcount) {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put(PROPERTY_NAME, trendcount);
        return agentInstanceContext.getStatementContext().getEventAdapterService().adapterForTypedMap(result, eventType);
    }

    /**
     * Creates the event type for this view.
     *
     * @return event type of view
     */
    protected static EventType createEventType(StatementContext statementContext) {
        Map<String, Object> eventTypeMap = new HashMap<String, Object>();
        eventTypeMap.put(PROPERTY_NAME, Long.class);
        return statementContext.getEventAdapterService().createAnonymousMapType("test", eventTypeMap, true);
    }
}
