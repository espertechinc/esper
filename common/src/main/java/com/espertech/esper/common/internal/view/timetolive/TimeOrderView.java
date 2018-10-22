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
package com.espertech.esper.common.internal.view.timetolive;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.AgentInstanceStopCallback;
import com.espertech.esper.common.internal.context.util.AgentInstanceStopServices;
import com.espertech.esper.common.internal.context.util.EPStatementHandleCallbackSchedule;
import com.espertech.esper.common.internal.epl.expression.time.eval.TimePeriodProvide;
import com.espertech.esper.common.internal.schedule.ScheduleHandleCallback;
import com.espertech.esper.common.internal.schedule.ScheduleObjectType;
import com.espertech.esper.common.internal.util.CollectionUtil;
import com.espertech.esper.common.internal.view.core.*;
import com.espertech.esper.common.internal.view.previous.IStreamSortRankRandomAccess;
import com.espertech.esper.common.internal.view.sort.SortWindowIterator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

/**
 * Window retaining timestamped events up to a given number of seconds such that
 * older events that arrive later are sorted into the window and released in timestamp order.
 * <p>
 * The insert stream consists of all arriving events. The remove stream consists of events in
 * order of timestamp value as supplied by each event.
 * <p>
 * Timestamp values on events should match runtime time. The window compares runtime time to timestamp value
 * and releases events when the event's timestamp is less then runtime time minus interval size (the
 * event is older then the window tail).
 * <p>
 * The view accepts 2 parameters. The first parameter is the field name to get the event timestamp value from,
 * the second parameter defines the interval size.
 */
public class TimeOrderView extends ViewSupport implements DataWindowView, AgentInstanceStopCallback {
    private final AgentInstanceContext agentInstanceContext;
    private final TimeOrderViewFactory factory;
    private final IStreamSortRankRandomAccess optionalSortedRandomAccess;
    private final EPStatementHandleCallbackSchedule handle;
    private final long scheduleSlot;
    private final TimePeriodProvide timePeriodProvide;

    private EventBean[] eventsPerStream = new EventBean[1];
    private TreeMap<Object, Object> sortedEvents;
    private boolean isCallbackScheduled;
    private int eventCount;

    public TimeOrderView(AgentInstanceViewFactoryChainContext agentInstanceContext,
                         TimeOrderViewFactory factory,
                         IStreamSortRankRandomAccess optionalSortedRandomAccess,
                         TimePeriodProvide timePeriodProvide) {
        this.agentInstanceContext = agentInstanceContext.getAgentInstanceContext();
        this.factory = factory;
        this.optionalSortedRandomAccess = optionalSortedRandomAccess;
        this.scheduleSlot = agentInstanceContext.getStatementContext().getScheduleBucket().allocateSlot();
        this.timePeriodProvide = timePeriodProvide;

        sortedEvents = new TreeMap<>();

        ScheduleHandleCallback callback = new ScheduleHandleCallback() {
            public void scheduledTrigger() {
                agentInstanceContext.getAuditProvider().scheduleFire(agentInstanceContext.getAgentInstanceContext(), ScheduleObjectType.view, factory.getViewName());
                agentInstanceContext.getInstrumentationProvider().qViewScheduledEval(factory);
                TimeOrderView.this.expire();
                agentInstanceContext.getInstrumentationProvider().aViewScheduledEval();
            }
        };
        handle = new EPStatementHandleCallbackSchedule(agentInstanceContext.getEpStatementAgentInstanceHandle(), callback);
    }

    public final EventType getEventType() {
        // The schema is the parent view's schema
        return parent.getEventType();
    }

    public final void update(EventBean[] newData, EventBean[] oldData) {
        agentInstanceContext.getAuditProvider().view(newData, oldData, agentInstanceContext, factory);
        agentInstanceContext.getInstrumentationProvider().qViewProcessIRStream(factory, newData, oldData);

        EventBean[] postOldEventsArray = null;

        // Remove old data
        if (oldData != null) {
            for (int i = 0; i < oldData.length; i++) {
                EventBean oldDataItem = oldData[i];
                Object sortValues = getTimestamp(oldDataItem);
                boolean result = CollectionUtil.removeEventByKeyLazyListMap(sortValues, oldDataItem, sortedEvents);
                if (result) {
                    eventCount--;
                    if (postOldEventsArray == null) {
                        postOldEventsArray = oldData;
                    } else {
                        postOldEventsArray = CollectionUtil.addArrayWithSetSemantics(postOldEventsArray, oldData);
                    }
                }
            }
        }

        if ((newData != null) && (newData.length > 0)) {
            // figure out the current tail time
            long runtimeTime = agentInstanceContext.getStatementContext().getSchedulingService().getTime();
            long windowTailTime = runtimeTime - timePeriodProvide.deltaAdd(runtimeTime, null, true, agentInstanceContext) + 1;
            long oldestEvent = Long.MAX_VALUE;
            if (!sortedEvents.isEmpty()) {
                oldestEvent = (Long) sortedEvents.firstKey();
            }
            boolean addedOlderEvent = false;

            // add events or post events as remove stream if already older then tail time
            ArrayList<EventBean> postOldEvents = null;
            for (int i = 0; i < newData.length; i++) {
                // get timestamp of event
                EventBean newEvent = newData[i];
                Long timestamp = getTimestamp(newEvent);

                // if the event timestamp indicates its older then the tail of the window, release it
                if (timestamp < windowTailTime) {
                    if (postOldEvents == null) {
                        postOldEvents = new ArrayList<EventBean>(2);
                    }
                    postOldEvents.add(newEvent);
                } else {
                    if (timestamp < oldestEvent) {
                        addedOlderEvent = true;
                        oldestEvent = timestamp;
                    }

                    // add to list
                    CollectionUtil.addEventByKeyLazyListMapBack(timestamp, newEvent, sortedEvents);
                    eventCount++;
                }
            }

            // If we do have data, check the callback
            if (!sortedEvents.isEmpty()) {
                // If we haven't scheduled a callback yet, schedule it now
                if (!isCallbackScheduled) {
                    long callbackWait = oldestEvent - windowTailTime + 1;
                    agentInstanceContext.getAuditProvider().scheduleAdd(callbackWait, agentInstanceContext, handle, ScheduleObjectType.view, factory.getViewName());
                    agentInstanceContext.getStatementContext().getSchedulingService().add(callbackWait, handle, scheduleSlot);
                    isCallbackScheduled = true;
                } else {
                    // We may need to reschedule, and older event may have been added
                    if (addedOlderEvent) {
                        oldestEvent = (Long) sortedEvents.firstKey();
                        long callbackWait = oldestEvent - windowTailTime + 1;
                        agentInstanceContext.getAuditProvider().scheduleRemove(agentInstanceContext, handle, ScheduleObjectType.view, factory.getViewName());
                        agentInstanceContext.getStatementContext().getSchedulingService().remove(handle, scheduleSlot);
                        agentInstanceContext.getAuditProvider().scheduleAdd(callbackWait, agentInstanceContext, handle, ScheduleObjectType.view, factory.getViewName());
                        agentInstanceContext.getStatementContext().getSchedulingService().add(callbackWait, handle, scheduleSlot);
                        isCallbackScheduled = true;
                    }
                }
            }

            if (postOldEvents != null) {
                postOldEventsArray = postOldEvents.toArray(new EventBean[postOldEvents.size()]);
            }

            if (optionalSortedRandomAccess != null) {
                optionalSortedRandomAccess.refresh(sortedEvents, eventCount, eventCount);
            }
        }

        // update child views
        if (child != null) {
            agentInstanceContext.getInstrumentationProvider().qViewIndicate(factory, newData, postOldEventsArray);
            child.update(newData, postOldEventsArray);
            agentInstanceContext.getInstrumentationProvider().aViewIndicate();
        }

        agentInstanceContext.getInstrumentationProvider().aViewProcessIRStream();
    }

    protected Long getTimestamp(EventBean newEvent) {
        eventsPerStream[0] = newEvent;
        return (Long) factory.timestampEval.evaluate(eventsPerStream, true, agentInstanceContext);
    }

    /**
     * True to indicate the sort window is empty, or false if not empty.
     *
     * @return true if empty sort window
     */
    public boolean isEmpty() {
        return sortedEvents.isEmpty();
    }

    public final Iterator<EventBean> iterator() {
        return new SortWindowIterator(sortedEvents);
    }

    public final String toString() {
        return this.getClass().getName();
    }

    public void visitView(ViewDataVisitor viewDataVisitor) {
        viewDataVisitor.visitPrimary(sortedEvents, false, factory.getViewName(), eventCount, null);
    }

    /**
     * This method removes (expires) objects from the window and schedules a new callback for the
     * time when the next oldest message would expire from the window.
     */
    protected final void expire() {
        long currentTime = agentInstanceContext.getStatementContext().getSchedulingService().getTime();
        long expireBeforeTimestamp = currentTime - timePeriodProvide.deltaSubtract(currentTime, null, true, agentInstanceContext) + 1;
        isCallbackScheduled = false;

        List<EventBean> releaseEvents = null;
        Long oldestKey;
        while (true) {
            if (sortedEvents.isEmpty()) {
                oldestKey = null;
                break;
            }

            oldestKey = (Long) sortedEvents.firstKey();
            if (oldestKey >= expireBeforeTimestamp) {
                break;
            }

            Object released = sortedEvents.remove(oldestKey);
            if (released != null) {
                if (released instanceof List) {
                    List<EventBean> releasedEventList = (List<EventBean>) released;
                    if (releaseEvents == null) {
                        releaseEvents = releasedEventList;
                    } else {
                        releaseEvents.addAll(releasedEventList);
                    }
                    eventCount -= releasedEventList.size();
                } else {
                    EventBean releasedEvent = (EventBean) released;
                    if (releaseEvents == null) {
                        releaseEvents = new ArrayList<EventBean>(4);
                    }
                    releaseEvents.add(releasedEvent);
                    eventCount--;
                }
            }
        }

        if (optionalSortedRandomAccess != null) {
            optionalSortedRandomAccess.refresh(sortedEvents, eventCount, eventCount);
        }

        // If there are child views, do the update method
        if (child != null) {
            if ((releaseEvents != null) && (!releaseEvents.isEmpty())) {
                EventBean[] oldEvents = releaseEvents.toArray(new EventBean[releaseEvents.size()]);
                agentInstanceContext.getInstrumentationProvider().qViewIndicate(factory, null, oldEvents);
                child.update(null, oldEvents);
                agentInstanceContext.getInstrumentationProvider().aViewIndicate();
            }
        }

        // If we still have events in the window, schedule new callback
        if (oldestKey == null) {
            return;
        }

        // Next callback
        long callbackWait = oldestKey - expireBeforeTimestamp + 1;
        agentInstanceContext.getAuditProvider().scheduleAdd(callbackWait, agentInstanceContext, handle, ScheduleObjectType.view, factory.getViewName());
        agentInstanceContext.getStatementContext().getSchedulingService().add(callbackWait, handle, scheduleSlot);
        isCallbackScheduled = true;
    }

    public void stop(AgentInstanceStopServices services) {
        if (handle != null) {
            agentInstanceContext.getAuditProvider().scheduleRemove(agentInstanceContext, handle, ScheduleObjectType.view, factory.getViewName());
            agentInstanceContext.getStatementContext().getSchedulingService().remove(handle, scheduleSlot);
        }
    }

    public ViewFactory getFactory() {
        return factory;
    }
}
