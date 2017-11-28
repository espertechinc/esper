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
import com.espertech.esper.collection.MultiKeyUntyped;
import com.espertech.esper.collection.OneEventCollection;
import com.espertech.esper.core.context.util.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.util.CollectionUtil;
import com.espertech.esper.view.DataWindowView;
import com.espertech.esper.view.ViewDataVisitor;
import com.espertech.esper.view.ViewFactory;
import com.espertech.esper.view.ViewSupport;

import java.util.*;

/**
 * Window sorting by values in the specified field extending a specified number of elements
 * from the lowest value up or the highest value down and retaining only the last unique value per key.
 * <p>
 * The type of the field to be sorted in the event must implement the Comparable interface.
 * <p>
 * The natural order in which events arrived is used as the second sorting criteria. Thus should events arrive
 * with equal sort values the oldest event leaves the sort window first.
 * <p>
 * Old values removed from a another view are removed from the sort view.
 */
public class RankWindowView extends ViewSupport implements DataWindowView {
    private final RankWindowViewFactory rankWindowViewFactory;
    private final EventBean[] eventsPerStream = new EventBean[1];
    private final int sortWindowSize;
    private final IStreamSortRankRandomAccess optionalRankedRandomAccess;
    protected final AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext;

    protected TreeMap<Object, Object> sortedEvents;   // key is computed sort-key, value is either List<EventBean> or EventBean
    protected Map<Object, Object> uniqueKeySortKeys;  // key is computed unique-key, value is computed sort-key
    protected int numberOfEvents;

    public RankWindowView(RankWindowViewFactory rankWindowViewFactory,
                          int sortWindowSize,
                          IStreamSortRankRandomAccess optionalRankedRandomAccess,
                          AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext) {
        this.rankWindowViewFactory = rankWindowViewFactory;
        this.sortWindowSize = sortWindowSize;
        this.optionalRankedRandomAccess = optionalRankedRandomAccess;
        this.agentInstanceViewFactoryContext = agentInstanceViewFactoryContext;

        sortedEvents = new TreeMap<>(rankWindowViewFactory.comparator);
        uniqueKeySortKeys = new HashMap<>();
    }

    public final EventType getEventType() {
        // The schema is the parent view's schema
        return parent.getEventType();
    }

    public final void update(EventBean[] newData, EventBean[] oldData) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qViewProcessIRStream(this, rankWindowViewFactory.getViewName(), newData, oldData);
        }

        OneEventCollection removedEvents = new OneEventCollection();

        // Remove old data
        if (oldData != null) {
            for (int i = 0; i < oldData.length; i++) {
                Object uniqueKey = getUniqueKey(oldData[i]);
                Object existingSortKey = uniqueKeySortKeys.get(uniqueKey);

                if (existingSortKey == null) {
                    continue;
                }

                EventBean event = removeFromSortedEvents(existingSortKey, uniqueKey);
                if (event != null) {
                    numberOfEvents--;
                    uniqueKeySortKeys.remove(uniqueKey);
                    removedEvents.add(event);
                    internalHandleRemovedKey(existingSortKey, oldData[i]);
                }
            }
        }

        // Add new data
        if (newData != null) {
            for (int i = 0; i < newData.length; i++) {
                Object uniqueKey = getUniqueKey(newData[i]);
                Object newSortKey = getSortValues(newData[i]);
                Object existingSortKey = uniqueKeySortKeys.get(uniqueKey);

                // not currently found: its a new entry
                if (existingSortKey == null) {
                    compareAndAddOrPassthru(newData[i], uniqueKey, newSortKey, removedEvents);
                } else {
                    // same unique-key event found already, remove and add again
                    // key did not change, perform in-place substitute of event
                    if (existingSortKey.equals(newSortKey)) {
                        EventBean replaced = inplaceReplaceSortedEvents(existingSortKey, uniqueKey, newData[i]);
                        if (replaced != null) {
                            removedEvents.add(replaced);
                        }
                        internalHandleReplacedKey(newSortKey, newData[i], replaced);
                    } else {
                        EventBean removed = removeFromSortedEvents(existingSortKey, uniqueKey);
                        if (removed != null) {
                            numberOfEvents--;
                            removedEvents.add(removed);
                            internalHandleRemovedKey(existingSortKey, removed);
                        }
                        compareAndAddOrPassthru(newData[i], uniqueKey, newSortKey, removedEvents);
                    }
                }
            }
        }

        // Remove data that sorts to the bottom of the window
        if (numberOfEvents > sortWindowSize) {
            while (numberOfEvents > sortWindowSize) {
                Object lastKey = sortedEvents.lastKey();
                Object existing = sortedEvents.get(lastKey);
                if (existing instanceof List) {
                    List<EventBean> existingList = (List<EventBean>) existing;
                    while (numberOfEvents > sortWindowSize && !existingList.isEmpty()) {
                        EventBean newestEvent = existingList.remove(0);
                        Object uniqueKey = getUniqueKey(newestEvent);
                        uniqueKeySortKeys.remove(uniqueKey);
                        numberOfEvents--;
                        removedEvents.add(newestEvent);
                        internalHandleRemovedKey(existing, newestEvent);
                    }
                    if (existingList.isEmpty()) {
                        sortedEvents.remove(lastKey);
                    }
                } else {
                    EventBean lastSortedEvent = (EventBean) existing;
                    Object uniqueKey = getUniqueKey(lastSortedEvent);
                    uniqueKeySortKeys.remove(uniqueKey);
                    numberOfEvents--;
                    removedEvents.add(lastSortedEvent);
                    sortedEvents.remove(lastKey);
                    internalHandleRemovedKey(lastKey, lastSortedEvent);
                }
            }
        }

        // If there are child views, fireStatementStopped update method
        if (optionalRankedRandomAccess != null) {
            optionalRankedRandomAccess.refresh(sortedEvents, numberOfEvents, sortWindowSize);
        }
        if (this.hasViews()) {
            EventBean[] expiredArr = null;
            if (!removedEvents.isEmpty()) {
                expiredArr = removedEvents.toArray();
            }

            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qViewIndicate(this, rankWindowViewFactory.getViewName(), newData, expiredArr);
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

    public void internalHandleReplacedKey(Object newSortKey, EventBean newEvent, EventBean oldEvent) {
        // no action
    }

    public void internalHandleRemovedKey(Object sortKey, EventBean eventBean) {
        // no action
    }

    public void internalHandleAddedKey(Object sortKey, EventBean eventBean) {
        // no action
    }

    private void compareAndAddOrPassthru(EventBean eventBean, Object uniqueKey, Object newSortKey, OneEventCollection removedEvents) {
        // determine full or not
        if (numberOfEvents >= sortWindowSize) {
            int compared = rankWindowViewFactory.comparator.compare(sortedEvents.lastKey(), newSortKey);

            // this new event will fall outside of the ranks or coincides with the last entry, so its an old event already
            if (compared < 0) {
                removedEvents.add(eventBean);
            } else {
                // this new event is higher in sort key then the last entry so we are interested
                uniqueKeySortKeys.put(uniqueKey, newSortKey);
                numberOfEvents++;
                CollectionUtil.addEventByKeyLazyListMapBack(newSortKey, eventBean, sortedEvents);
                internalHandleAddedKey(newSortKey, eventBean);
            }
        } else {
            // not yet filled, need to add
            uniqueKeySortKeys.put(uniqueKey, newSortKey);
            numberOfEvents++;
            CollectionUtil.addEventByKeyLazyListMapBack(newSortKey, eventBean, sortedEvents);
            internalHandleAddedKey(newSortKey, eventBean);
        }
    }

    private EventBean removeFromSortedEvents(Object sortKey, Object uniqueKeyToRemove) {
        Object existing = sortedEvents.get(sortKey);

        EventBean removedOldEvent = null;
        if (existing != null) {
            if (existing instanceof List) {
                List<EventBean> existingList = (List<EventBean>) existing;
                Iterator<EventBean> it = existingList.iterator();
                for (; it.hasNext(); ) {
                    EventBean eventForRank = it.next();
                    if (getUniqueKey(eventForRank).equals(uniqueKeyToRemove)) {
                        it.remove();
                        removedOldEvent = eventForRank;
                        break;
                    }
                }

                if (existingList.isEmpty()) {
                    sortedEvents.remove(sortKey);
                }
            } else {
                removedOldEvent = (EventBean) existing;
                sortedEvents.remove(sortKey);
            }
        }
        return removedOldEvent;
    }

    private EventBean inplaceReplaceSortedEvents(Object sortKey, Object uniqueKeyToReplace, EventBean newData) {
        Object existing = sortedEvents.get(sortKey);

        EventBean replaced = null;
        if (existing != null) {
            if (existing instanceof List) {
                List<EventBean> existingList = (List<EventBean>) existing;
                Iterator<EventBean> it = existingList.iterator();
                for (; it.hasNext(); ) {
                    EventBean eventForRank = it.next();
                    if (getUniqueKey(eventForRank).equals(uniqueKeyToReplace)) {
                        it.remove();
                        replaced = eventForRank;
                        break;
                    }
                }
                existingList.add(newData);  // add to back as this is now the newest event
            } else {
                replaced = (EventBean) existing;
                sortedEvents.put(sortKey, newData);
            }
        }
        return replaced;
    }

    public final Iterator<EventBean> iterator() {
        return new RankWindowIterator(sortedEvents);
    }

    public final String toString() {
        return this.getClass().getName() +
                " uniqueFieldName=" + Arrays.toString(rankWindowViewFactory.uniqueCriteriaExpressions) +
                " sortFieldName=" + Arrays.toString(rankWindowViewFactory.sortCriteriaExpressions) +
                " isDescending=" + Arrays.toString(rankWindowViewFactory.isDescendingValues) +
                " sortWindowSize=" + sortWindowSize;
    }

    public Object getUniqueKey(EventBean theEvent) {
        return getUniqueKey(eventsPerStream, rankWindowViewFactory.uniqueEvals, theEvent, agentInstanceViewFactoryContext);
    }

    public Object getSortValues(EventBean theEvent) {
        return getSortKey(eventsPerStream, rankWindowViewFactory.sortEvals, theEvent, agentInstanceViewFactoryContext);
    }

    public static Object getUniqueKey(EventBean[] eventsPerStream, ExprEvaluator[] evaluators, EventBean theEvent, ExprEvaluatorContext evalContext) {
        eventsPerStream[0] = theEvent;
        if (evaluators.length > 1) {
            return getCriteriaMultiKey(eventsPerStream, evaluators, evalContext);
        } else {
            return evaluators[0].evaluate(eventsPerStream, true, evalContext);
        }
    }

    public static Object getSortKey(EventBean[] eventsPerStream, ExprEvaluator[] evaluators, EventBean theEvent, ExprEvaluatorContext evalContext) {
        eventsPerStream[0] = theEvent;
        if (evaluators.length > 1) {
            return getSortMultiKey(eventsPerStream, evaluators, evalContext);
        } else {
            return evaluators[0].evaluate(eventsPerStream, true, evalContext);
        }
    }

    public static MultiKeyUntyped getCriteriaMultiKey(EventBean[] eventsPerStream, ExprEvaluator[] evaluators, ExprEvaluatorContext evalContext) {
        Object[] result = new Object[evaluators.length];
        int count = 0;
        for (ExprEvaluator expr : evaluators) {
            result[count++] = expr.evaluate(eventsPerStream, true, evalContext);
        }
        return new MultiKeyUntyped(result);
    }

    public static HashableMultiKey getSortMultiKey(EventBean[] eventsPerStream, ExprEvaluator[] evaluators, ExprEvaluatorContext evalContext) {
        Object[] result = new Object[evaluators.length];
        int count = 0;
        for (ExprEvaluator expr : evaluators) {
            result[count++] = expr.evaluate(eventsPerStream, true, evalContext);
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
        viewDataVisitor.visitPrimary(sortedEvents, false, rankWindowViewFactory.getViewName(), numberOfEvents, sortedEvents.size());
    }

    public ViewFactory getViewFactory() {
        return rankWindowViewFactory;
    }
}
