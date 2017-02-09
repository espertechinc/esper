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
import com.espertech.esper.collection.MultiKeyUntyped;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.agg.rollup.GroupByRollupKey;
import com.espertech.esper.epl.agg.service.AggregationService;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.util.*;

/**
 * An order-by processor that sorts events according to the expressions
 * in the order_by clause.
 */
public class OrderByProcessorImpl implements OrderByProcessor {

    private final OrderByProcessorFactoryImpl factory;
    private final AggregationService aggregationService;

    public OrderByProcessorImpl(OrderByProcessorFactoryImpl factory, AggregationService aggregationService) {
        this.factory = factory;
        this.aggregationService = aggregationService;
    }

    public Object getSortKey(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        return getSortKeyInternal(eventsPerStream, isNewData, exprEvaluatorContext, factory.getOrderBy());
    }

    public Object getSortKey(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext, OrderByElement[] elementsForLevel) {
        return getSortKeyInternal(eventsPerStream, isNewData, exprEvaluatorContext, elementsForLevel);
    }

    private static Object getSortKeyInternal(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext, OrderByElement[] elements) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qOrderBy(eventsPerStream, elements);
        }

        if (elements.length == 1) {
            if (InstrumentationHelper.ENABLED) {
                Object value = elements[0].getExpr().evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
                InstrumentationHelper.get().aOrderBy(value);
                return value;
            }
            return elements[0].getExpr().evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        }

        Object[] values = new Object[elements.length];
        int count = 0;
        for (OrderByElement sortPair : elements) {
            values[count++] = sortPair.getExpr().evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        }

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aOrderBy(values);
        }
        return new MultiKeyUntyped(values);
    }

    public Object[] getSortKeyPerRow(EventBean[] generatingEvents, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (generatingEvents == null) {
            return null;
        }

        Object[] sortProperties = new Object[generatingEvents.length];

        int count = 0;
        EventBean[] evalEventsPerStream = new EventBean[1];

        if (factory.getOrderBy().length == 1) {
            ExprEvaluator singleEval = factory.getOrderBy()[0].getExpr();
            for (EventBean theEvent : generatingEvents) {
                evalEventsPerStream[0] = theEvent;
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().qOrderBy(evalEventsPerStream, factory.getOrderBy());
                }
                sortProperties[count] = singleEval.evaluate(evalEventsPerStream, isNewData, exprEvaluatorContext);
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aOrderBy(sortProperties[count]);
                }
                count++;
            }
        } else {
            for (EventBean theEvent : generatingEvents) {
                Object[] values = new Object[factory.getOrderBy().length];
                int countTwo = 0;
                evalEventsPerStream[0] = theEvent;
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().qOrderBy(evalEventsPerStream, factory.getOrderBy());
                }
                for (OrderByElement sortPair : factory.getOrderBy()) {
                    values[countTwo++] = sortPair.getExpr().evaluate(evalEventsPerStream, isNewData, exprEvaluatorContext);
                }
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aOrderBy(values);
                }

                sortProperties[count] = new MultiKeyUntyped(values);
                count++;
            }
        }

        return sortProperties;
    }

    public EventBean[] sort(EventBean[] outgoingEvents, EventBean[][] generatingEvents, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (outgoingEvents == null || outgoingEvents.length < 2) {
            return outgoingEvents;
        }

        // Get the group by keys if needed
        Object[] groupByKeys = null;
        if (factory.isNeedsGroupByKeys()) {
            groupByKeys = generateGroupKeys(generatingEvents, isNewData, exprEvaluatorContext);
        }

        return sort(outgoingEvents, generatingEvents, groupByKeys, isNewData, exprEvaluatorContext);
    }

    public EventBean[] sort(EventBean[] outgoingEvents, List<GroupByRollupKey> currentGenerators, boolean isNewData, AgentInstanceContext exprEvaluatorContext, OrderByElement[][] elementsPerLevel) {
        List<Object> sortValuesMultiKeys = createSortPropertiesWRollup(currentGenerators, elementsPerLevel, isNewData, exprEvaluatorContext);
        return sortInternal(outgoingEvents, sortValuesMultiKeys, factory.getComparator());
    }

    public EventBean[] sort(EventBean[] outgoingEvents, EventBean[][] generatingEvents, Object[] groupByKeys, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (outgoingEvents == null || outgoingEvents.length < 2) {
            return outgoingEvents;
        }

        // Create the multikeys of sort values
        List<Object> sortValuesMultiKeys = createSortProperties(generatingEvents, groupByKeys, isNewData, exprEvaluatorContext);

        return sortInternal(outgoingEvents, sortValuesMultiKeys, factory.getComparator());
    }

    private List<Object> createSortProperties(EventBean[][] generatingEvents, Object[] groupByKeys, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        Object[] sortProperties = new Object[generatingEvents.length];

        OrderByElement[] elements = factory.getOrderBy();
        if (elements.length == 1) {
            int count = 0;
            for (EventBean[] eventsPerStream : generatingEvents) {
                // Make a new multikey that contains the sort-by values.
                if (factory.isNeedsGroupByKeys()) {
                    aggregationService.setCurrentAccess(groupByKeys[count], exprEvaluatorContext.getAgentInstanceId(), null);
                }

                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().qOrderBy(eventsPerStream, factory.getOrderBy());
                }
                sortProperties[count] = elements[0].getExpr().evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aOrderBy(sortProperties[count]);
                }
                count++;
            }
        } else {
            int count = 0;
            for (EventBean[] eventsPerStream : generatingEvents) {
                // Make a new multikey that contains the sort-by values.
                if (factory.isNeedsGroupByKeys()) {
                    aggregationService.setCurrentAccess(groupByKeys[count], exprEvaluatorContext.getAgentInstanceId(), null);
                }

                Object[] values = new Object[factory.getOrderBy().length];
                int countTwo = 0;
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().qOrderBy(eventsPerStream, factory.getOrderBy());
                }
                for (OrderByElement sortPair : factory.getOrderBy()) {
                    values[countTwo++] = sortPair.getExpr().evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
                }
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aOrderBy(values);
                }

                sortProperties[count] = new MultiKeyUntyped(values);
                count++;
            }
        }
        return Arrays.asList(sortProperties);
    }

    public EventBean[] sort(EventBean[] outgoingEvents, Object[] orderKeys, ExprEvaluatorContext exprEvaluatorContext) {
        TreeMap<Object, Object> sort = new TreeMap<Object, Object>(factory.getComparator());

        if (outgoingEvents == null || outgoingEvents.length < 2) {
            return outgoingEvents;
        }

        for (int i = 0; i < outgoingEvents.length; i++) {
            Object entry = sort.get(orderKeys[i]);
            if (entry == null) {
                sort.put(orderKeys[i], outgoingEvents[i]);
            } else if (entry instanceof EventBean) {
                List<EventBean> list = new ArrayList<EventBean>();
                list.add((EventBean) entry);
                list.add(outgoingEvents[i]);
                sort.put(orderKeys[i], list);
            } else {
                List<EventBean> list = (List<EventBean>) entry;
                list.add(outgoingEvents[i]);
            }
        }

        EventBean[] result = new EventBean[outgoingEvents.length];
        int count = 0;
        for (Object entry : sort.values()) {
            if (entry instanceof List) {
                List<EventBean> output = (List<EventBean>) entry;
                for (EventBean theEvent : output) {
                    result[count++] = theEvent;
                }
            } else {
                result[count++] = (EventBean) entry;
            }
        }
        return result;
    }

    private Object[] generateGroupKeys(EventBean[][] generatingEvents, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        Object[] keys = new Object[generatingEvents.length];

        int count = 0;
        for (EventBean[] eventsPerStream : generatingEvents) {
            keys[count++] = generateGroupKey(eventsPerStream, isNewData, exprEvaluatorContext);
        }

        return keys;
    }

    private Object generateGroupKey(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        ExprEvaluator[] evals = factory.getGroupByNodes();
        if (evals.length == 1) {
            return evals[0].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        }

        Object[] keys = new Object[evals.length];
        int count = 0;
        for (ExprEvaluator exprNode : evals) {
            keys[count] = exprNode.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
            count++;
        }

        return new MultiKeyUntyped(keys);
    }

    private List<Object> createSortPropertiesWRollup(List<GroupByRollupKey> currentGenerators, OrderByElement[][] elementsPerLevel, boolean isNewData, AgentInstanceContext exprEvaluatorContext) {
        Object[] sortProperties = new Object[currentGenerators.size()];

        OrderByElement[] elements = factory.getOrderBy();
        if (elements.length == 1) {
            int count = 0;
            for (GroupByRollupKey rollup : currentGenerators) {

                // Make a new multikey that contains the sort-by values.
                if (factory.isNeedsGroupByKeys()) {
                    aggregationService.setCurrentAccess(rollup.getGroupKey(), exprEvaluatorContext.getAgentInstanceId(), rollup.getLevel());
                }

                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().qOrderBy(rollup.getGenerator(), factory.getOrderBy());
                }
                sortProperties[count] = elementsPerLevel[rollup.getLevel().getLevelNumber()][0].getExpr().evaluate(rollup.getGenerator(), isNewData, exprEvaluatorContext);
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aOrderBy(sortProperties[count]);
                }

                count++;
            }
        } else {
            int count = 0;
            for (GroupByRollupKey rollup : currentGenerators) {

                // Make a new multikey that contains the sort-by values.
                if (factory.isNeedsGroupByKeys()) {
                    aggregationService.setCurrentAccess(rollup.getGroupKey(), exprEvaluatorContext.getAgentInstanceId(), rollup.getLevel());
                }

                Object[] values = new Object[factory.getOrderBy().length];
                int countTwo = 0;
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().qOrderBy(rollup.getGenerator(), factory.getOrderBy());
                }
                for (OrderByElement sortPair : elementsPerLevel[rollup.getLevel().getLevelNumber()]) {
                    values[countTwo++] = sortPair.getExpr().evaluate(rollup.getGenerator(), isNewData, exprEvaluatorContext);
                }
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aOrderBy(values);
                }

                sortProperties[count] = new MultiKeyUntyped(values);
                count++;
            }
        }
        return Arrays.asList(sortProperties);
    }

    private static EventBean[] sortInternal(EventBean[] outgoingEvents, List<Object> sortValuesMultiKeys, Comparator<Object> comparator) {
        // Map the sort values to the corresponding outgoing events
        Map<Object, List<EventBean>> sortToOutgoing = new HashMap<Object, List<EventBean>>();
        int countOne = 0;
        for (Object sortValues : sortValuesMultiKeys) {
            List<EventBean> list = sortToOutgoing.get(sortValues);
            if (list == null) {
                list = new ArrayList<EventBean>();
            }
            list.add(outgoingEvents[countOne++]);
            sortToOutgoing.put(sortValues, list);
        }

        // Sort the sort values
        Collections.sort(sortValuesMultiKeys, comparator);

        // Sort the outgoing events in the same order
        Set<Object> sortSet = new LinkedHashSet<Object>(sortValuesMultiKeys);
        EventBean[] result = new EventBean[outgoingEvents.length];
        int countTwo = 0;
        for (Object sortValues : sortSet) {
            Collection<EventBean> output = sortToOutgoing.get(sortValues);
            for (EventBean theEvent : output) {
                result[countTwo++] = theEvent;
            }
        }

        return result;
    }

    public EventBean determineLocalMinMax(EventBean[] outgoingEvents, EventBean[][] generatingEvents, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {

        // Get the group by keys if needed
        Object[] groupByKeys = null;
        if (factory.isNeedsGroupByKeys()) {
            groupByKeys = generateGroupKeys(generatingEvents, isNewData, exprEvaluatorContext);
        }

        OrderByElement[] elements = factory.getOrderBy();
        Object localMinMax = null;
        EventBean outgoingMinMaxBean = null;

        if (elements.length == 1) {
            int count = 0;
            for (EventBean[] eventsPerStream : generatingEvents) {
                if (factory.isNeedsGroupByKeys()) {
                    aggregationService.setCurrentAccess(groupByKeys[count], exprEvaluatorContext.getAgentInstanceId(), null);
                }

                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().qOrderBy(eventsPerStream, factory.getOrderBy());
                }
                Object sortKey = elements[0].getExpr().evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aOrderBy(localMinMax);
                }

                boolean newMinMax = localMinMax == null || factory.getComparator().compare(localMinMax, sortKey) > 0;
                if (newMinMax) {
                    localMinMax = sortKey;
                    outgoingMinMaxBean = outgoingEvents[count];
                }

                count++;
            }
        } else {
            int count = 0;
            Object[] values = new Object[factory.getOrderBy().length];
            MultiKeyUntyped valuesMk = new MultiKeyUntyped(values);

            for (EventBean[] eventsPerStream : generatingEvents) {
                if (factory.isNeedsGroupByKeys()) {
                    aggregationService.setCurrentAccess(groupByKeys[count], exprEvaluatorContext.getAgentInstanceId(), null);
                }

                int countTwo = 0;
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().qOrderBy(eventsPerStream, factory.getOrderBy());
                }
                for (OrderByElement sortPair : factory.getOrderBy()) {
                    values[countTwo++] = sortPair.getExpr().evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
                }
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aOrderBy(values);
                }

                boolean newMinMax = localMinMax == null || factory.getComparator().compare(localMinMax, valuesMk) > 0;
                if (newMinMax) {
                    localMinMax = valuesMk;
                    values = new Object[factory.getOrderBy().length];
                    valuesMk = new MultiKeyUntyped(values);
                    outgoingMinMaxBean = outgoingEvents[count];
                }

                count++;
            }
        }

        return outgoingMinMaxBean;
    }

    public EventBean determineLocalMinMax(EventBean[] outgoingEvents, Object[] orderKeys) {
        Object localMinMax = null;
        EventBean outgoingMinMaxBean = null;

        for (int i = 0; i < outgoingEvents.length; i++) {
            boolean newMinMax = localMinMax == null || factory.getComparator().compare(localMinMax, orderKeys[i]) > 0;
            if (newMinMax) {
                localMinMax = orderKeys[i];
                outgoingMinMaxBean = outgoingEvents[i];
            }
        }

        return outgoingMinMaxBean;
    }
}
