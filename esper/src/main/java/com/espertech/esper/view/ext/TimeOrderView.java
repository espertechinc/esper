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
package com.espertech.esper.view.ext;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.core.context.util.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.core.service.EPStatementHandleCallback;
import com.espertech.esper.core.service.EngineLevelExtensionServicesContext;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.time.ExprTimePeriodEvalDeltaConst;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.schedule.ScheduleHandleCallback;
import com.espertech.esper.util.CollectionUtil;
import com.espertech.esper.util.StopCallback;
import com.espertech.esper.view.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * Timestamp values on events should match engine time. The window compares engine time to timestamp value
 * and releases events when the event's timestamp is less then engine time minus interval size (the
 * event is older then the window tail).
 * <p>
 * The view accepts 2 parameters. The first parameter is the field name to get the event timestamp value from,
 * the second parameter defines the interval size.
 */
public class TimeOrderView extends ViewSupport implements DataWindowView, StoppableView, StopCallback {
    protected final AgentInstanceViewFactoryChainContext agentInstanceContext;
    private final ViewFactory viewFactory;
    private final ExprNode timestampExpression;
    private final ExprEvaluator timestampEvaluator;
    protected final ExprTimePeriodEvalDeltaConst timeDeltaComputation;
    protected final IStreamSortRankRandomAccess optionalSortedRandomAccess;
    protected final long scheduleSlot;
    protected final EPStatementHandleCallback handle;

    private EventBean[] eventsPerStream = new EventBean[1];
    protected TreeMap<Object, Object> sortedEvents;
    protected boolean isCallbackScheduled;
    protected int eventCount;

    public TimeOrderView(AgentInstanceViewFactoryChainContext agentInstanceContext,
                         ViewFactory viewFactory,
                         ExprNode timestampExpr,
                         ExprEvaluator timestampEvaluator,
                         ExprTimePeriodEvalDeltaConst timeDeltaComputation,
                         IStreamSortRankRandomAccess optionalSortedRandomAccess) {
        this.agentInstanceContext = agentInstanceContext;
        this.viewFactory = viewFactory;
        this.timestampExpression = timestampExpr;
        this.timestampEvaluator = timestampEvaluator;
        this.timeDeltaComputation = timeDeltaComputation;
        this.optionalSortedRandomAccess = optionalSortedRandomAccess;
        this.scheduleSlot = agentInstanceContext.getStatementContext().getScheduleBucket().allocateSlot();

        sortedEvents = new TreeMap<Object, Object>();

        ScheduleHandleCallback callback = new ScheduleHandleCallback() {
            public void scheduledTrigger(EngineLevelExtensionServicesContext extensionServicesContext) {
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().qViewScheduledEval(TimeOrderView.this, TimeOrderView.this.viewFactory.getViewName());
                }
                TimeOrderView.this.expire();
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aViewScheduledEval();
                }
            }
        };
        handle = new EPStatementHandleCallback(agentInstanceContext.getEpStatementAgentInstanceHandle(), callback);
        agentInstanceContext.addTerminationCallback(this);
    }

    /**
     * Returns the timestamp property name.
     *
     * @return property name supplying timestamp values
     */
    public ExprNode getTimestampExpression() {
        return timestampExpression;
    }

    public ExprTimePeriodEvalDeltaConst getTimeDeltaComputation() {
        return timeDeltaComputation;
    }

    public final EventType getEventType() {
        // The schema is the parent view's schema
        return parent.getEventType();
    }

    public final void update(EventBean[] newData, EventBean[] oldData) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qViewProcessIRStream(this, viewFactory.getViewName(), newData, oldData);
        }

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
                    internalHandleRemoved(sortValues, oldDataItem);
                }
            }
        }

        if ((newData != null) && (newData.length > 0)) {
            // figure out the current tail time
            long engineTime = agentInstanceContext.getStatementContext().getSchedulingService().getTime();
            long windowTailTime = engineTime - timeDeltaComputation.deltaAdd(engineTime) + 1;
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
                    internalHandleAdd(timestamp, newEvent);
                }
            }

            // If we do have data, check the callback
            if (!sortedEvents.isEmpty()) {
                // If we haven't scheduled a callback yet, schedule it now
                if (!isCallbackScheduled) {
                    long callbackWait = oldestEvent - windowTailTime + 1;
                    agentInstanceContext.getStatementContext().getSchedulingService().add(callbackWait, handle, scheduleSlot);
                    isCallbackScheduled = true;
                } else {
                    // We may need to reschedule, and older event may have been added
                    if (addedOlderEvent) {
                        oldestEvent = (Long) sortedEvents.firstKey();
                        long callbackWait = oldestEvent - windowTailTime + 1;
                        agentInstanceContext.getStatementContext().getSchedulingService().remove(handle, scheduleSlot);
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
        if (this.hasViews()) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qViewIndicate(this, viewFactory.getViewName(), newData, postOldEventsArray);
            }
            updateChildren(newData, postOldEventsArray);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aViewIndicate();
            }
        }

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aViewProcessIRStream();
        }
    }

    public void internalHandleAdd(Object sortValues, EventBean newDataItem) {
        // no action required
    }

    public void internalHandleRemoved(Object sortValues, EventBean oldDataItem) {
        // no action required
    }

    public void internalHandleExpired(Object sortValues, EventBean oldDataItem) {
        // no action required
    }

    public void internalHandleExpired(Object sortValues, List<EventBean> oldDataItems) {
        // no action required
    }

    protected Long getTimestamp(EventBean newEvent) {
        eventsPerStream[0] = newEvent;
        return (Long) timestampEvaluator.evaluate(eventsPerStream, true, agentInstanceContext);
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
        return this.getClass().getName() +
                " timestampExpression=" + timestampExpression;
    }

    public void visitView(ViewDataVisitor viewDataVisitor) {
        viewDataVisitor.visitPrimary(sortedEvents, false, viewFactory.getViewName(), eventCount, null);
    }

    /**
     * This method removes (expires) objects from the window and schedules a new callback for the
     * time when the next oldest message would expire from the window.
     */
    protected final void expire() {
        long currentTime = agentInstanceContext.getStatementContext().getSchedulingService().getTime();
        long expireBeforeTimestamp = currentTime - timeDeltaComputation.deltaSubtract(currentTime) + 1;
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
                    internalHandleExpired(oldestKey, releasedEventList);
                } else {
                    EventBean releasedEvent = (EventBean) released;
                    if (releaseEvents == null) {
                        releaseEvents = new ArrayList<EventBean>(4);
                    }
                    releaseEvents.add(releasedEvent);
                    eventCount--;
                    internalHandleExpired(oldestKey, releasedEvent);
                }
            }
        }

        if (optionalSortedRandomAccess != null) {
            optionalSortedRandomAccess.refresh(sortedEvents, eventCount, eventCount);
        }

        // If there are child views, do the update method
        if (this.hasViews()) {
            if ((releaseEvents != null) && (!releaseEvents.isEmpty())) {
                EventBean[] oldEvents = releaseEvents.toArray(new EventBean[releaseEvents.size()]);
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().qViewIndicate(this, viewFactory.getViewName(), null, oldEvents);
                }
                updateChildren(null, oldEvents);
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aViewIndicate();
                }
            }
        }

        // If we still have events in the window, schedule new callback
        if (oldestKey == null) {
            return;
        }

        // Next callback
        long callbackWait = oldestKey - expireBeforeTimestamp + 1;
        agentInstanceContext.getStatementContext().getSchedulingService().add(callbackWait, handle, scheduleSlot);
        isCallbackScheduled = true;
    }

    public void stopView() {
        stopSchedule();
        agentInstanceContext.removeTerminationCallback(this);
    }

    public void stop() {
        stopSchedule();
    }

    public void stopSchedule() {
        if (handle != null) {
            agentInstanceContext.getStatementContext().getSchedulingService().remove(handle, scheduleSlot);
        }
    }

    public ViewFactory getViewFactory() {
        return viewFactory;
    }

    private static final Logger log = LoggerFactory.getLogger(TimeOrderView.class);
}
