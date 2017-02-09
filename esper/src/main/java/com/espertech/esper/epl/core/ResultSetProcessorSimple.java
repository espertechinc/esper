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
import com.espertech.esper.client.EventType;
import com.espertech.esper.collection.ArrayEventIterator;
import com.espertech.esper.collection.MultiKey;
import com.espertech.esper.collection.TransformEventIterator;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.util.CollectionUtil;
import com.espertech.esper.view.Viewable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Result set processor for the simplest case: no aggregation functions used in the select clause, and no group-by.
 * <p>
 * The processor generates one row for each event entering (new event) and one row for each event leaving (old event).
 */
public class ResultSetProcessorSimple extends ResultSetProcessorBaseSimple {
    protected final ResultSetProcessorSimpleFactory prototype;
    private final SelectExprProcessor selectExprProcessor;
    private final OrderByProcessor orderByProcessor;
    protected ExprEvaluatorContext exprEvaluatorContext;
    private ResultSetProcessorSimpleOutputLastHelper outputLastHelper;
    private ResultSetProcessorSimpleOutputAllHelper outputAllHelper;

    public ResultSetProcessorSimple(ResultSetProcessorSimpleFactory prototype, SelectExprProcessor selectExprProcessor, OrderByProcessor orderByProcessor, AgentInstanceContext agentInstanceContext) {
        this.prototype = prototype;
        this.selectExprProcessor = selectExprProcessor;
        this.orderByProcessor = orderByProcessor;
        this.exprEvaluatorContext = agentInstanceContext;
        if (prototype.isOutputLast()) { // output-last always uses this mechanism
            outputLastHelper = prototype.getResultSetProcessorHelperFactory().makeRSSimpleOutputLast(prototype, this, agentInstanceContext);
        } else if (prototype.isOutputAll() && prototype.isEnableOutputLimitOpt()) {
            outputAllHelper = prototype.getResultSetProcessorHelperFactory().makeRSSimpleOutputAll(prototype, this, agentInstanceContext);
        }
    }

    public void setAgentInstanceContext(AgentInstanceContext context) {
        exprEvaluatorContext = context;
    }

    public EventType getResultEventType() {
        return prototype.getResultEventType();
    }

    public UniformPair<EventBean[]> processJoinResult(Set<MultiKey<EventBean>> newEvents, Set<MultiKey<EventBean>> oldEvents, boolean isSynthesize) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qResultSetProcessSimple();
        }

        EventBean[] selectOldEvents = null;
        EventBean[] selectNewEvents;

        if (prototype.getOptionalHavingExpr() == null) {
            if (prototype.isSelectRStream()) {
                if (orderByProcessor == null) {
                    selectOldEvents = ResultSetProcessorUtil.getSelectJoinEventsNoHaving(selectExprProcessor, oldEvents, false, isSynthesize, exprEvaluatorContext);
                } else {
                    selectOldEvents = ResultSetProcessorUtil.getSelectJoinEventsNoHavingWithOrderBy(selectExprProcessor, orderByProcessor, oldEvents, false, isSynthesize, exprEvaluatorContext);
                }
            }

            if (orderByProcessor == null) {
                selectNewEvents = ResultSetProcessorUtil.getSelectJoinEventsNoHaving(selectExprProcessor, newEvents, true, isSynthesize, exprEvaluatorContext);
            } else {
                selectNewEvents = ResultSetProcessorUtil.getSelectJoinEventsNoHavingWithOrderBy(selectExprProcessor, orderByProcessor, newEvents, true, isSynthesize, exprEvaluatorContext);
            }
        } else {
            if (prototype.isSelectRStream()) {
                if (orderByProcessor == null) {
                    selectOldEvents = ResultSetProcessorUtil.getSelectJoinEventsHaving(selectExprProcessor, oldEvents, prototype.getOptionalHavingExpr(), false, isSynthesize, exprEvaluatorContext);
                } else {
                    selectOldEvents = ResultSetProcessorUtil.getSelectJoinEventsHavingWithOrderBy(selectExprProcessor, orderByProcessor, oldEvents, prototype.getOptionalHavingExpr(), false, isSynthesize, exprEvaluatorContext);
                }
            }

            if (orderByProcessor == null) {
                selectNewEvents = ResultSetProcessorUtil.getSelectJoinEventsHaving(selectExprProcessor, newEvents, prototype.getOptionalHavingExpr(), true, isSynthesize, exprEvaluatorContext);
            } else {
                selectNewEvents = ResultSetProcessorUtil.getSelectJoinEventsHavingWithOrderBy(selectExprProcessor, orderByProcessor, newEvents, prototype.getOptionalHavingExpr(), true, isSynthesize, exprEvaluatorContext);
            }
        }

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aResultSetProcessSimple(selectNewEvents, selectOldEvents);
        }
        return new UniformPair<EventBean[]>(selectNewEvents, selectOldEvents);
    }

    public UniformPair<EventBean[]> processViewResult(EventBean[] newData, EventBean[] oldData, boolean isSynthesize) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qResultSetProcessSimple();
        }

        EventBean[] selectOldEvents = null;
        EventBean[] selectNewEvents;
        if (prototype.getOptionalHavingExpr() == null) {
            if (prototype.isSelectRStream()) {
                if (orderByProcessor == null) {
                    selectOldEvents = ResultSetProcessorUtil.getSelectEventsNoHaving(selectExprProcessor, oldData, false, isSynthesize, exprEvaluatorContext);
                } else {
                    selectOldEvents = ResultSetProcessorUtil.getSelectEventsNoHavingWithOrderBy(selectExprProcessor, orderByProcessor, oldData, false, isSynthesize, exprEvaluatorContext);
                }
            }

            if (orderByProcessor == null) {
                selectNewEvents = ResultSetProcessorUtil.getSelectEventsNoHaving(selectExprProcessor, newData, true, isSynthesize, exprEvaluatorContext);
            } else {
                selectNewEvents = ResultSetProcessorUtil.getSelectEventsNoHavingWithOrderBy(selectExprProcessor, orderByProcessor, newData, true, isSynthesize, exprEvaluatorContext);
            }
        } else {
            if (prototype.isSelectRStream()) {
                if (orderByProcessor == null) {
                    selectOldEvents = ResultSetProcessorUtil.getSelectEventsHaving(selectExprProcessor, oldData, prototype.getOptionalHavingExpr(), false, isSynthesize, exprEvaluatorContext);
                } else {
                    selectOldEvents = ResultSetProcessorUtil.getSelectEventsHavingWithOrderBy(selectExprProcessor, orderByProcessor, oldData, prototype.getOptionalHavingExpr(), false, isSynthesize, exprEvaluatorContext);
                }
            }
            if (orderByProcessor == null) {
                selectNewEvents = ResultSetProcessorUtil.getSelectEventsHaving(selectExprProcessor, newData, prototype.getOptionalHavingExpr(), true, isSynthesize, exprEvaluatorContext);
            } else {
                selectNewEvents = ResultSetProcessorUtil.getSelectEventsHavingWithOrderBy(selectExprProcessor, orderByProcessor, newData, prototype.getOptionalHavingExpr(), true, isSynthesize, exprEvaluatorContext);
            }
        }

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aResultSetProcessSimple(selectNewEvents, selectOldEvents);
        }
        return new UniformPair<EventBean[]>(selectNewEvents, selectOldEvents);
    }

    /**
     * Process view results for the iterator.
     *
     * @param newData new events
     * @return pair of insert and remove stream
     */
    public UniformPair<EventBean[]> processViewResultIterator(EventBean[] newData) {
        EventBean[] selectNewEvents;
        if (prototype.getOptionalHavingExpr() == null) {
            // ignore orderByProcessor
            selectNewEvents = ResultSetProcessorUtil.getSelectEventsNoHaving(selectExprProcessor, newData, true, true, exprEvaluatorContext);
        } else {
            // ignore orderByProcessor
            selectNewEvents = ResultSetProcessorUtil.getSelectEventsHaving(selectExprProcessor, newData, prototype.getOptionalHavingExpr(), true, true, exprEvaluatorContext);
        }

        return new UniformPair<EventBean[]>(selectNewEvents, null);
    }

    public Iterator<EventBean> getIterator(Viewable parent) {
        if (orderByProcessor != null) {
            // Pull all events, generate order keys
            EventBean[] eventsPerStream = new EventBean[1];
            List<EventBean> events = new ArrayList<EventBean>();
            List<Object> orderKeys = new ArrayList<Object>();
            Iterator parentIterator = parent.iterator();
            if (parentIterator == null) {
                return CollectionUtil.NULL_EVENT_ITERATOR;
            }
            for (EventBean aParent : parent) {
                eventsPerStream[0] = aParent;
                Object orderKey = orderByProcessor.getSortKey(eventsPerStream, true, exprEvaluatorContext);
                UniformPair<EventBean[]> pair = processViewResultIterator(eventsPerStream);
                EventBean[] result = pair.getFirst();
                if (result != null && result.length != 0) {
                    events.add(result[0]);
                }
                orderKeys.add(orderKey);
            }

            // sort
            EventBean[] outgoingEvents = events.toArray(new EventBean[events.size()]);
            Object[] orderKeysArr = orderKeys.toArray(new Object[orderKeys.size()]);
            EventBean[] orderedEvents = orderByProcessor.sort(outgoingEvents, orderKeysArr, exprEvaluatorContext);

            return new ArrayEventIterator(orderedEvents);
        }
        // Return an iterator that gives row-by-row a result
        return new TransformEventIterator(parent.iterator(), new ResultSetProcessorSimpleTransform(this));
    }

    public Iterator<EventBean> getIterator(Set<MultiKey<EventBean>> joinSet) {
        // Process join results set as a regular join, includes sorting and having-clause filter
        UniformPair<EventBean[]> result = processJoinResult(joinSet, CollectionUtil.EMPTY_ROW_SET, true);
        return new ArrayEventIterator(result.getFirst());
    }

    public void clear() {
        // No need to clear state, there is no state held
    }

    public boolean hasAggregation() {
        return false;
    }

    public void applyViewResult(EventBean[] newData, EventBean[] oldData) {
    }

    public void applyJoinResult(Set<MultiKey<EventBean>> newEvents, Set<MultiKey<EventBean>> oldEvents) {
    }

    public void processOutputLimitedLastAllNonBufferedView(EventBean[] newData, EventBean[] oldData, boolean isGenerateSynthetic, boolean isAll) {
        if (isAll) {
            outputAllHelper.processView(newData, oldData);
        } else {
            outputLastHelper.processView(newData, oldData);
        }
    }

    public void processOutputLimitedLastAllNonBufferedJoin(Set<MultiKey<EventBean>> newEvents, Set<MultiKey<EventBean>> oldEvents, boolean isGenerateSynthetic, boolean isAll) {
        if (isAll) {
            outputAllHelper.processJoin(newEvents, oldEvents);
        } else {
            outputLastHelper.processJoin(newEvents, oldEvents);
        }
    }

    public UniformPair<EventBean[]> continueOutputLimitedLastAllNonBufferedView(boolean isSynthesize, boolean isAll) {
        if (isAll) {
            return outputAllHelper.outputView(isSynthesize);
        }
        return outputLastHelper.outputView(isSynthesize);
    }

    public UniformPair<EventBean[]> continueOutputLimitedLastAllNonBufferedJoin(boolean isSynthesize, boolean isAll) {
        if (isAll) {
            return outputAllHelper.outputJoin(isSynthesize);
        }
        return outputLastHelper.outputJoin(isSynthesize);
    }

    public void stop() {
        if (outputLastHelper != null) {
            outputLastHelper.destroy();
        }
        if (outputAllHelper != null) {
            outputAllHelper.destroy();
        }
    }

    public void acceptHelperVisitor(ResultSetProcessorOutputHelperVisitor visitor) {
        if (outputLastHelper != null) {
            visitor.visit(outputLastHelper);
        }
        if (outputAllHelper != null) {
            visitor.visit(outputAllHelper);
        }
    }
}
