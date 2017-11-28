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
package com.espertech.esper.example.ohlc;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.core.context.util.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.core.service.EPStatementHandleCallback;
import com.espertech.esper.core.service.EngineLevelExtensionServicesContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.schedule.ScheduleHandleCallback;
import com.espertech.esper.view.ViewSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;

/**
 * Custom view to compute minute OHLC bars for double values and based on the event's timestamps.
 * <p>
 * Assumes events arrive in the order of timestamps, i.e. event 1 timestamp is always less or equal event 2 timestamp.
 * <p>
 * Implemented as a custom plug-in view rather then a series of EPL statements for the following reasons:
 * - Custom output result mixing aggregation (min/max) and first/last values
 * - No need for a data window retaining events if using a custom view
 * - Unlimited number of groups (minute timestamps) makes the group-by clause hard to use
 */
public class OHLCBarPlugInView extends ViewSupport {
    private final static int LATE_EVENT_SLACK_SECONDS = 5;
    private final static Logger log = LoggerFactory.getLogger(OHLCBarPlugInView.class);

    private final AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext;
    private final long scheduleSlot;
    private final ExprNode timestampExpression;
    private final ExprNode valueExpression;
    private final EventBean[] eventsPerStream = new EventBean[1];

    private EPStatementHandleCallback handle;
    private Long cutoffTimestampMinute;
    private Long currentTimestampMinute;
    private Double first;
    private Double last;
    private Double max;
    private Double min;
    private EventBean lastEvent;

    public OHLCBarPlugInView(AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext, ExprNode timestampExpression, ExprNode valueExpression) {
        this.agentInstanceViewFactoryContext = agentInstanceViewFactoryContext;
        this.timestampExpression = timestampExpression;
        this.valueExpression = valueExpression;
        this.scheduleSlot = agentInstanceViewFactoryContext.getStatementContext().getScheduleBucket().allocateSlot();
    }

    public void update(EventBean[] newData, EventBean[] oldData) {
        if (newData == null) {
            return;
        }

        for (EventBean theEvent : newData) {
            eventsPerStream[0] = theEvent;
            Long timestamp = (Long) timestampExpression.getForge().getExprEvaluator().evaluate(eventsPerStream, true, agentInstanceViewFactoryContext);
            Long timestampMinute = removeSeconds(timestamp);
            double value = (Double) valueExpression.getForge().getExprEvaluator().evaluate(eventsPerStream, true, agentInstanceViewFactoryContext);

            // test if this minute has already been published, the event is too late
            if ((cutoffTimestampMinute != null) && (timestampMinute <= cutoffTimestampMinute)) {
                continue;
            }

            // if the same minute, aggregate
            if (timestampMinute.equals(currentTimestampMinute)) {
                applyValue(value);
            } else {
                // first time we see an event for this minute
                // there is data to post
                if (currentTimestampMinute != null) {
                    postData();
                }

                currentTimestampMinute = timestampMinute;
                applyValue(value);

                // schedule a callback to fire in case no more events arrive
                scheduleCallback();
            }
        }
    }

    public EventType getEventType() {
        return getEventType(agentInstanceViewFactoryContext.getStatementContext().getEventAdapterService());
    }

    public Iterator<EventBean> iterator() {
        throw new UnsupportedOperationException("Not supported");
    }

    private void applyValue(double value) {
        if (first == null) {
            first = value;
        }
        last = value;
        if (min == null) {
            min = value;
        } else if (min.compareTo(value) > 0) {
            min = value;
        }
        if (max == null) {
            max = value;
        } else if (max.compareTo(value) < 0) {
            max = value;
        }
    }

    protected static EventType getEventType(EventAdapterService eventAdapterService) {
        return eventAdapterService.addBeanType(OHLCBarValue.class.getName(), OHLCBarValue.class, false, false, false);
    }

    private static long removeSeconds(long timestamp) {
        Calendar cal = GregorianCalendar.getInstance();
        cal.setTimeInMillis(timestamp);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private void scheduleCallback() {
        if (handle != null) {
            // remove old schedule
            agentInstanceViewFactoryContext.getStatementContext().getSchedulingService().remove(handle, scheduleSlot);
            handle = null;
        }

        long currentTime = agentInstanceViewFactoryContext.getStatementContext().getSchedulingService().getTime();
        long currentRemoveSeconds = removeSeconds(currentTime);
        long targetTime = currentRemoveSeconds + (60 + LATE_EVENT_SLACK_SECONDS) * 1000; // leave some seconds for late comers
        long scheduleAfterMSec = targetTime - currentTime;

        ScheduleHandleCallback callback = new ScheduleHandleCallback() {
            public void scheduledTrigger(EngineLevelExtensionServicesContext extensionServicesContext) {
                handle = null;  // clear out schedule handle
                OHLCBarPlugInView.this.postData();
            }
        };

        handle = new EPStatementHandleCallback(agentInstanceViewFactoryContext.getEpStatementAgentInstanceHandle(), callback);
        agentInstanceViewFactoryContext.getStatementContext().getSchedulingService().add(scheduleAfterMSec, handle, scheduleSlot);
    }

    private void postData() {
        OHLCBarValue barValue = new OHLCBarValue(currentTimestampMinute, first, last, max, min);
        EventBean outgoing = agentInstanceViewFactoryContext.getStatementContext().getEventAdapterService().adapterForBean(barValue);
        if (lastEvent == null) {
            this.updateChildren(new EventBean[]{outgoing}, null);
        } else {
            this.updateChildren(new EventBean[]{outgoing}, new EventBean[]{lastEvent});
        }
        lastEvent = outgoing;

        cutoffTimestampMinute = currentTimestampMinute;
        first = null;
        last = null;
        max = null;
        min = null;
        currentTimestampMinute = null;
    }
}
