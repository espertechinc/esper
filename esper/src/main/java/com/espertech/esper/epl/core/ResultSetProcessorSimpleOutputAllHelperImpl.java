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
package com.espertech.esper.epl.core;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.collection.MultiKey;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.event.EventBeanUtility;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;

public class ResultSetProcessorSimpleOutputAllHelperImpl implements ResultSetProcessorSimpleOutputAllHelper {
    private final ResultSetProcessorSimple processor;

    private final Deque<EventBean> eventsNewView = new ArrayDeque<EventBean>(2);
    private final Deque<EventBean> eventsOldView = new ArrayDeque<EventBean>(2);
    private final Deque<MultiKey<EventBean>> eventsNewJoin = new ArrayDeque<MultiKey<EventBean>>(2);
    private final Deque<MultiKey<EventBean>> eventsOldJoin = new ArrayDeque<MultiKey<EventBean>>(2);

    public ResultSetProcessorSimpleOutputAllHelperImpl(ResultSetProcessorSimple processor) {
        this.processor = processor;
    }

    public void processView(EventBean[] newData, EventBean[] oldData) {
        if (processor.prototype.getOptionalHavingExpr() == null) {
            addToView(newData, oldData);
            return;
        }

        EventBean[] eventsPerStream = new EventBean[1];
        if (newData != null && newData.length > 0) {
            for (EventBean theEvent : newData) {
                eventsPerStream[0] = theEvent;

                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().qHavingClauseNonJoin(theEvent);
                }
                Boolean passesHaving = (Boolean) processor.prototype.getOptionalHavingExpr().evaluate(eventsPerStream, true, processor.exprEvaluatorContext);
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aHavingClauseNonJoin(passesHaving);
                }
                if ((passesHaving == null) || (!passesHaving)) {
                    continue;
                }
                eventsNewView.add(theEvent);
            }
        }
        if (oldData != null && oldData.length > 0) {
            for (EventBean theEvent : oldData) {
                eventsPerStream[0] = theEvent;

                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().qHavingClauseNonJoin(theEvent);
                }
                Boolean passesHaving = (Boolean) processor.prototype.getOptionalHavingExpr().evaluate(eventsPerStream, false, processor.exprEvaluatorContext);
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aHavingClauseNonJoin(passesHaving);
                }
                if ((passesHaving == null) || (!passesHaving)) {
                    continue;
                }
                eventsOldView.add(theEvent);
            }
        }
    }

    public void processJoin(Set<MultiKey<EventBean>> newEvents, Set<MultiKey<EventBean>> oldEvents) {
        if (processor.prototype.getOptionalHavingExpr() == null) {
            addToJoin(newEvents, oldEvents);
            return;
        }

        if (newEvents != null && newEvents.size() > 0) {
            for (MultiKey<EventBean> theEvent : newEvents) {
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().qHavingClauseJoin(theEvent.getArray());
                }
                Boolean passesHaving = (Boolean) processor.prototype.getOptionalHavingExpr().evaluate(theEvent.getArray(), true, processor.exprEvaluatorContext);
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aHavingClauseJoin(passesHaving);
                }
                if ((passesHaving == null) || (!passesHaving)) {
                    continue;
                }
                eventsNewJoin.add(theEvent);
            }
        }
        if (oldEvents != null && oldEvents.size() > 0) {
            for (MultiKey<EventBean> theEvent : oldEvents) {

                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().qHavingClauseJoin(theEvent.getArray());
                }
                Boolean passesHaving = (Boolean) processor.prototype.getOptionalHavingExpr().evaluate(theEvent.getArray(), false, processor.exprEvaluatorContext);
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aHavingClauseJoin(passesHaving);
                }
                if ((passesHaving == null) || (!passesHaving)) {
                    continue;
                }
                eventsOldJoin.add(theEvent);
            }
        }
    }

    public UniformPair<EventBean[]> outputView(boolean isSynthesize) {
        UniformPair<EventBean[]> pair = processor.processViewResult(EventBeanUtility.toArrayNullIfEmpty(eventsNewView), EventBeanUtility.toArrayNullIfEmpty(eventsOldView), isSynthesize);
        eventsNewView.clear();
        eventsOldView.clear();
        return pair;
    }

    public UniformPair<EventBean[]> outputJoin(boolean isSynthesize) {
        UniformPair<EventBean[]> pair = processor.processJoinResult(EventBeanUtility.toLinkedHashSetNullIfEmpty(eventsNewJoin), EventBeanUtility.toLinkedHashSetNullIfEmpty(eventsOldJoin), isSynthesize);
        eventsNewJoin.clear();
        eventsOldJoin.clear();
        return pair;
    }

    public void destroy() {
        // no action required
    }

    private void addToView(EventBean[] newData, EventBean[] oldData) {
        EventBeanUtility.addToCollection(newData, eventsNewView);
        EventBeanUtility.addToCollection(oldData, eventsOldView);
    }

    private void addToJoin(Set<MultiKey<EventBean>> newEvents, Set<MultiKey<EventBean>> oldEvents) {
        EventBeanUtility.addToCollection(newEvents, eventsNewJoin);
        EventBeanUtility.addToCollection(oldEvents, eventsOldJoin);
    }
}
