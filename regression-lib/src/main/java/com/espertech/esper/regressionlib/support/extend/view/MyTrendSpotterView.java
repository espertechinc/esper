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
package com.espertech.esper.regressionlib.support.extend.view;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.collection.SingleEventIterator;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.view.core.ViewSupport;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MyTrendSpotterView extends ViewSupport {
    private static final String PROPERTY_NAME = "trendcount";

    private final MyTrendSpotterViewFactory factory;
    private final AgentInstanceContext agentInstanceContext;
    private final EventBean[] eventsPerStream = new EventBean[1];

    private Long trendcount;
    private Double lastDataPoint;

    // The remove stream must post the same object event reference
    private EventBean lastInsertStreamEvent;

    /**
     * Constructor requires the name of the field to use in the parent view to compute a trend.
     *
     * @param factory              is the factory
     * @param agentInstanceContext contains required view services
     */
    public MyTrendSpotterView(MyTrendSpotterViewFactory factory, AgentInstanceContext agentInstanceContext) {
        this.factory = factory;
        this.agentInstanceContext = agentInstanceContext;
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
                double dataPoint = ((Number) factory.getParameter().evaluate(eventsPerStream, true, null)).doubleValue();

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

        if (child != null) {
            EventBean newDataPost = populateMap(trendcount);
            lastInsertStreamEvent = newDataPost;
            child.update(new EventBean[]{newDataPost}, removeStreamToPost);
        }
    }

    public final EventType getEventType() {
        return factory.getEventType();
    }

    public final Iterator<EventBean> iterator() {
        EventBean theEvent = populateMap(trendcount);
        return new SingleEventIterator(theEvent);
    }

    private EventBean populateMap(Long trendcount) {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put(PROPERTY_NAME, trendcount);
        return agentInstanceContext.getEventBeanTypedEventFactory().adapterForTypedMap(result, factory.getEventType());
    }
}
