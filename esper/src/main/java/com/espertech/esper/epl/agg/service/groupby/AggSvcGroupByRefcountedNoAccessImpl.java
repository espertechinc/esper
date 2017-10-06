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
package com.espertech.esper.epl.agg.service.groupby;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.agg.aggregator.AggregationMethod;
import com.espertech.esper.epl.agg.service.common.*;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.util.*;

/**
 * Implementation for handling aggregation with grouping by group-keys.
 */
public class AggSvcGroupByRefcountedNoAccessImpl extends AggregationServiceBaseGrouped {
    // maintain for each group a row of aggregator states that the expression node canb pull the data from via index
    protected Map<Object, AggregationMethodRow> aggregatorsPerGroup;

    // maintain a current row for random access into the aggregator state table
    // (row=groups, columns=expression nodes that have aggregation functions)
    private AggregationMethod[] currentAggregatorRow;
    private Object currentGroupKey;

    private List<Object> removedKeys;

    /**
     * Ctor.
     *
     * @param evaluators - evaluate the sub-expression within the aggregate function (ie. sum(4*myNum))
     * @param prototypes - collect the aggregation state that evaluators evaluate to, act as prototypes for new aggregations
     *                   aggregation states for each group
     */
    public AggSvcGroupByRefcountedNoAccessImpl(ExprEvaluator[] evaluators,
                                               AggregationMethodFactory[] prototypes) {
        super(evaluators, prototypes);
        this.aggregatorsPerGroup = new HashMap<Object, AggregationMethodRow>();
        removedKeys = new ArrayList<Object>();
    }

    public void clearResults(ExprEvaluatorContext exprEvaluatorContext) {
        aggregatorsPerGroup.clear();
    }

    public void applyEnter(EventBean[] eventsPerStream, Object groupByKey, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qAggregationGroupedApplyEnterLeave(true, aggregators.length, 0, groupByKey);
        }
        handleRemovedKeys();

        AggregationMethodRow row = aggregatorsPerGroup.get(groupByKey);

        // The methodFactories for this group do not exist, need to create them from the prototypes
        AggregationMethod[] groupAggregators;
        if (row == null) {
            groupAggregators = AggSvcGroupByUtil.newAggregators(aggregators);
            row = new AggregationMethodRow(1, groupAggregators);
            aggregatorsPerGroup.put(groupByKey, row);
        } else {
            groupAggregators = row.getMethods();
            row.increaseRefcount();
        }

        // For this row, evaluate sub-expressions, enter result
        currentAggregatorRow = groupAggregators;
        for (int i = 0; i < evaluators.length; i++) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qAggNoAccessEnterLeave(true, i, groupAggregators[i], aggregators[i].getAggregationExpression());
            }
            Object columnResult = evaluators[i].evaluate(eventsPerStream, true, exprEvaluatorContext);
            groupAggregators[i].enter(columnResult);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aAggNoAccessEnterLeave(true, i, groupAggregators[i]);
            }
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aAggregationGroupedApplyEnterLeave(true);
        }
    }

    public void applyLeave(EventBean[] eventsPerStream, Object groupByKey, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qAggregationGroupedApplyEnterLeave(false, aggregators.length, 0, groupByKey);
        }
        AggregationMethodRow row = aggregatorsPerGroup.get(groupByKey);

        // The methodFactories for this group do not exist, need to create them from the prototypes
        AggregationMethod[] groupAggregators;
        if (row != null) {
            groupAggregators = row.getMethods();
        } else {
            groupAggregators = AggSvcGroupByUtil.newAggregators(aggregators);
            row = new AggregationMethodRow(1, groupAggregators);
            aggregatorsPerGroup.put(groupByKey, row);
        }

        // For this row, evaluate sub-expressions, enter result
        currentAggregatorRow = groupAggregators;
        for (int i = 0; i < evaluators.length; i++) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qAggNoAccessEnterLeave(false, i, groupAggregators[i], aggregators[i].getAggregationExpression());
            }
            Object columnResult = evaluators[i].evaluate(eventsPerStream, false, exprEvaluatorContext);
            groupAggregators[i].leave(columnResult);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aAggNoAccessEnterLeave(false, i, groupAggregators[i]);
            }
        }

        row.decreaseRefcount();
        if (row.getRefcount() <= 0) {
            removedKeys.add(groupByKey);
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aAggregationGroupedApplyEnterLeave(false);
        }
    }

    public void setCurrentAccess(Object groupByKey, int agentInstanceId, AggregationGroupByRollupLevel rollupLevel) {
        AggregationMethodRow row = aggregatorsPerGroup.get(groupByKey);

        if (row != null) {
            currentAggregatorRow = row.getMethods();
        } else {
            currentAggregatorRow = null;
        }

        if (currentAggregatorRow == null) {
            currentAggregatorRow = AggSvcGroupByUtil.newAggregators(aggregators);
        }
        currentGroupKey = groupByKey;
    }

    public Object getValue(int column, int agentInstanceId, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        return currentAggregatorRow[column].getValue();
    }

    public Collection<EventBean> getCollectionOfEvents(int column, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return null;
    }

    public Collection<Object> getCollectionScalar(int column, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return null;
    }

    public EventBean getEventBean(int column, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return null;
    }

    public void setRemovedCallback(AggregationRowRemovedCallback callback) {
        // not applicable
    }

    public void accept(AggregationServiceVisitor visitor) {
        visitor.visitAggregations(aggregatorsPerGroup.size(), aggregatorsPerGroup);
    }

    public void acceptGroupDetail(AggregationServiceVisitorWGroupDetail visitor) {
        visitor.visitGrouped(aggregatorsPerGroup.size());
        for (Map.Entry<Object, AggregationMethodRow> entry : aggregatorsPerGroup.entrySet()) {
            visitor.visitGroup(entry.getKey(), entry.getValue());
        }
    }

    public boolean isGrouped() {
        return true;
    }

    public Object getGroupKey(int agentInstanceId) {
        return currentGroupKey;
    }

    protected void handleRemovedKeys() {
        // we collect removed keys lazily on the next enter to reduce the chance of empty-group queries creating empty methodFactories temporarily
        if (!removedKeys.isEmpty()) {
            for (Object removedKey : removedKeys) {
                aggregatorsPerGroup.remove(removedKey);
            }
            removedKeys.clear();
        }
    }

    public Collection<Object> getGroupKeys(ExprEvaluatorContext exprEvaluatorContext) {
        handleRemovedKeys();
        return aggregatorsPerGroup.keySet();
    }
}
