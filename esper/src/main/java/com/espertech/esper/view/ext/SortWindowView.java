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
import com.espertech.esper.collection.HashableMultiKey;
import com.espertech.esper.collection.OneEventCollection;
import com.espertech.esper.core.context.util.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.util.CollectionUtil;
import com.espertech.esper.view.*;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

/**
 * Window sorting by values in the specified field extending a specified number of elements
 * from the lowest value up or the highest value down.
 * The view accepts 3 parameters. The first parameter is the field name to get the values to sort for,
 * the second parameter defines whether to sort ascending or descending, the third parameter
 * is the number of elements to keep in the sorted list.
 * <p>
 * The type of the field to be sorted in the event must implement the Comparable interface.
 * <p>
 * The natural order in which events arrived is used as the second sorting criteria. Thus should events arrive
 * with equal sort values the oldest event leaves the sort window first.
 * <p>
 * Old values removed from a prior view are removed from the sort view.
 */
public class SortWindowView extends ViewSupport implements DataWindowView {
    private final SortWindowViewFactory sortWindowViewFactory;
    private final EventBean[] eventsPerStream = new EventBean[1];
    private final int sortWindowSize;
    private final IStreamSortRankRandomAccess optionalSortedRandomAccess;
    protected final AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext;

    protected TreeMap<Object, Object> sortedEvents;
    protected int eventCount;

    public SortWindowView(SortWindowViewFactory sortWindowViewFactory,
                          int sortWindowSize,
                          IStreamSortRankRandomAccess optionalSortedRandomAccess,
                          AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext) {
        this.sortWindowViewFactory = sortWindowViewFactory;
        this.sortWindowSize = sortWindowSize;
        this.optionalSortedRandomAccess = optionalSortedRandomAccess;
        this.agentInstanceViewFactoryContext = agentInstanceViewFactoryContext;

        sortedEvents = new TreeMap<Object, Object>(sortWindowViewFactory.comparator);
    }

    /**
     * Returns the field names supplying the values to sort by.
     *
     * @return field names to sort by
     */
    protected final ExprNode[] getSortCriteriaExpressions() {
        return sortWindowViewFactory.sortCriteriaExpressions;
    }

    /**
     * Returns the flags indicating whether to sort in descending order on each property.
     *
     * @return the isDescending value for each sort property
     */
    protected final boolean[] getIsDescendingValues() {
        return sortWindowViewFactory.isDescendingValues;
    }

    /**
     * Returns the number of elements kept by the sort window.
     *
     * @return size of window
     */
    protected final int getSortWindowSize() {
        return sortWindowSize;
    }

    public final EventType getEventType() {
        // The schema is the parent view's schema
        return parent.getEventType();
    }

    public final void update(EventBean[] newData, EventBean[] oldData) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qViewProcessIRStream(this, sortWindowViewFactory.getViewName(), newData, oldData);
        }

        OneEventCollection removedEvents = null;

        // Remove old data
        if (oldData != null) {
            for (int i = 0; i < oldData.length; i++) {
                EventBean oldDataItem = oldData[i];
                Object sortValues = getSortValues(oldDataItem);
                boolean result = CollectionUtil.removeEventByKeyLazyListMap(sortValues, oldDataItem, sortedEvents);
                if (result) {
                    eventCount--;
                    if (removedEvents == null) {
                        removedEvents = new OneEventCollection();
                    }
                    removedEvents.add(oldDataItem);
                    internalHandleRemoved(sortValues, oldDataItem);
                }
            }
        }

        // Add new data
        if (newData != null) {
            for (int i = 0; i < newData.length; i++) {
                EventBean newDataItem = newData[i];
                Object sortValues = getSortValues(newDataItem);
                CollectionUtil.addEventByKeyLazyListMapFront(sortValues, newDataItem, sortedEvents);
                eventCount++;
                internalHandleAdd(sortValues, newDataItem);
            }
        }

        // Remove data that sorts to the bottom of the window
        if (eventCount > sortWindowSize) {
            int removeCount = eventCount - sortWindowSize;
            for (int i = 0; i < removeCount; i++) {
                // Remove the last element of the last key - sort order is key and then natural order of arrival
                Object lastKey = sortedEvents.lastKey();
                Object lastEntry = sortedEvents.get(lastKey);
                if (lastEntry instanceof List) {
                    List<EventBean> events = (List<EventBean>) lastEntry;
                    EventBean theEvent = events.remove(events.size() - 1);  // remove oldest event, newest events are first in list
                    eventCount--;
                    if (events.isEmpty()) {
                        sortedEvents.remove(lastKey);
                    }
                    if (removedEvents == null) {
                        removedEvents = new OneEventCollection();
                    }
                    removedEvents.add(theEvent);
                    internalHandleRemoved(lastKey, theEvent);
                } else {
                    EventBean theEvent = (EventBean) lastEntry;
                    eventCount--;
                    sortedEvents.remove(lastKey);
                    if (removedEvents == null) {
                        removedEvents = new OneEventCollection();
                    }
                    removedEvents.add(theEvent);
                    internalHandleRemoved(lastKey, theEvent);
                }
            }
        }

        // If there are child views, fireStatementStopped update method
        if (optionalSortedRandomAccess != null) {
            optionalSortedRandomAccess.refresh(sortedEvents, eventCount, sortWindowSize);
        }

        if (this.hasViews()) {
            EventBean[] expiredArr = null;
            if (removedEvents != null) {
                expiredArr = removedEvents.toArray();
            }

            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qViewIndicate(this, sortWindowViewFactory.getViewName(), newData, expiredArr);
            }
            updateChildren(newData, expiredArr);
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

    public final Iterator<EventBean> iterator() {
        return new SortWindowIterator(sortedEvents);
    }

    public final String toString() {
        return this.getClass().getName() +
                " sortFieldName=" + Arrays.toString(sortWindowViewFactory.sortCriteriaExpressions) +
                " isDescending=" + Arrays.toString(sortWindowViewFactory.isDescendingValues) +
                " sortWindowSize=" + sortWindowSize;
    }

    protected Object getSortValues(EventBean theEvent) {
        eventsPerStream[0] = theEvent;
        if (sortWindowViewFactory.sortCriteriaExpressions.length == 1) {
            return sortWindowViewFactory.sortCriteriaEvaluators[0].evaluate(eventsPerStream, true, agentInstanceViewFactoryContext);
        }

        Object[] result = new Object[sortWindowViewFactory.sortCriteriaExpressions.length];
        int count = 0;
        for (ExprEvaluator expr : sortWindowViewFactory.sortCriteriaEvaluators) {
            result[count++] = expr.evaluate(eventsPerStream, true, agentInstanceViewFactoryContext);
        }
        return new HashableMultiKey(result);
    }

    /**
     * True to indicate the sort window is empty, or false if not empty.
     *
     * @return true if empty sort window
     */
    public boolean isEmpty() {
        if (sortedEvents == null) {
            return true;
        }
        return sortedEvents.isEmpty();
    }

    public void visitView(ViewDataVisitor viewDataVisitor) {
        viewDataVisitor.visitPrimary(sortedEvents, false, sortWindowViewFactory.getViewName(), eventCount, null);
    }

    public ViewFactory getViewFactory() {
        return sortWindowViewFactory;
    }
}
