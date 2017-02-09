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

import java.util.Set;

public class ResultSetProcessorSimpleOutputLastHelperImpl implements ResultSetProcessorSimpleOutputLastHelper {
    private final ResultSetProcessorSimple processor;

    private EventBean outputLastIStreamBufView;
    private EventBean outputLastRStreamBufView;
    private MultiKey<EventBean> outputLastIStreamBufJoin;
    private MultiKey<EventBean> outputLastRStreamBufJoin;

    public ResultSetProcessorSimpleOutputLastHelperImpl(ResultSetProcessorSimple processor) {
        this.processor = processor;
    }

    public void processView(EventBean[] newData, EventBean[] oldData) {
        if (processor.prototype.getOptionalHavingExpr() == null) {
            if (newData != null && newData.length > 0) {
                outputLastIStreamBufView = newData[newData.length - 1];
            }
            if (oldData != null && oldData.length > 0) {
                outputLastRStreamBufView = oldData[oldData.length - 1];
            }
        } else {
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
                    outputLastIStreamBufView = theEvent;
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
                    outputLastRStreamBufView = theEvent;
                }
            }
        }
    }

    public void processJoin(Set<MultiKey<EventBean>> newEvents, Set<MultiKey<EventBean>> oldEvents) {
        if (processor.prototype.getOptionalHavingExpr() == null) {
            if (newEvents != null && !newEvents.isEmpty()) {
                outputLastIStreamBufJoin = EventBeanUtility.getLastInSet(newEvents);
            }
            if (oldEvents != null && !oldEvents.isEmpty()) {
                outputLastRStreamBufJoin = EventBeanUtility.getLastInSet(oldEvents);
            }
        } else {
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
                    outputLastIStreamBufJoin = theEvent;
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
                    outputLastRStreamBufJoin = theEvent;
                }
            }
        }
    }

    public UniformPair<EventBean[]> outputView(boolean isSynthesize) {
        if (outputLastIStreamBufView == null && outputLastRStreamBufView == null) {
            return null;
        }
        UniformPair<EventBean[]> pair = processor.processViewResult(EventBeanUtility.toArrayIfNotNull(outputLastIStreamBufView), EventBeanUtility.toArrayIfNotNull(outputLastRStreamBufView), isSynthesize);
        outputLastIStreamBufView = null;
        outputLastRStreamBufView = null;
        return pair;
    }

    public UniformPair<EventBean[]> outputJoin(boolean isSynthesize) {
        if (outputLastIStreamBufJoin == null && outputLastRStreamBufJoin == null) {
            return null;
        }
        UniformPair<EventBean[]> pair = processor.processJoinResult(EventBeanUtility.toSingletonSetIfNotNull(outputLastIStreamBufJoin), EventBeanUtility.toSingletonSetIfNotNull(outputLastRStreamBufJoin), isSynthesize);
        outputLastIStreamBufJoin = null;
        outputLastRStreamBufJoin = null;
        return pair;
    }

    public void destroy() {
        // no action required
    }
}
