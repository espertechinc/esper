/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.epl.core;

import com.espertech.esper.client.EventType;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.agg.rollup.GroupByRollupPerLevelExpression;
import com.espertech.esper.epl.agg.service.AggregationGroupByRollupDesc;
import com.espertech.esper.epl.agg.service.AggregationService;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.spec.OutputLimitLimitType;
import com.espertech.esper.epl.spec.OutputLimitSpec;

/**
 * Result set processor prototype for the fully-grouped case:
 * there is a group-by and all non-aggregation event properties in the select clause are listed in the group by,
 * and there are aggregation functions.
 */
public class ResultSetProcessorRowPerGroupRollupFactory implements ResultSetProcessorFactory
{
    private final GroupByRollupPerLevelExpression perLevelExpression;
    private final ExprNode[] groupKeyNodeExpressions;
    private final ExprEvaluator groupKeyNode;
    private final ExprEvaluator[] groupKeyNodes;
    private final boolean isSorting;
    private final boolean isSelectRStream;
    private final boolean isUnidirectional;
    private final OutputLimitSpec outputLimitSpec;
    private final boolean noDataWindowSingleSnapshot;
    private final AggregationGroupByRollupDesc groupByRollupDesc;
    private final boolean isJoin;
    private final boolean isHistoricalOnly;

    /**
     * Ctor.
     * @param groupKeyNodes - list of group-by expression nodes needed for building the group-by keys
     * Aggregation functions in the having node must have been pointed to the AggregationService for evaluation.
     * @param isSelectRStream - true if remove stream events should be generated
     * @param isUnidirectional - true if unidirectional join
     */
    public ResultSetProcessorRowPerGroupRollupFactory(GroupByRollupPerLevelExpression perLevelExpression,
                                                      ExprNode[] groupKeyNodeExpressions,
                                                      ExprEvaluator[] groupKeyNodes,
                                                      boolean isSelectRStream,
                                                      boolean isUnidirectional,
                                                      OutputLimitSpec outputLimitSpec,
                                                      boolean isSorting,
                                                      boolean noDataWindowSingleStream,
                                                      AggregationGroupByRollupDesc groupByRollupDesc,
                                                      boolean isJoin,
                                                      boolean isHistoricalOnly,
                                                      boolean iterateUnbounded)
    {
        this.groupKeyNodeExpressions = groupKeyNodeExpressions;
        this.perLevelExpression = perLevelExpression;
        this.groupKeyNodes = groupKeyNodes;
        if (groupKeyNodes.length == 1) {
            this.groupKeyNode = groupKeyNodes[0];
        }
        else {
            this.groupKeyNode = null;
        }
        this.isSorting = isSorting;
        this.isSelectRStream = isSelectRStream;
        this.isUnidirectional = isUnidirectional;
        this.outputLimitSpec = outputLimitSpec;
        this.noDataWindowSingleSnapshot = iterateUnbounded || (outputLimitSpec != null && outputLimitSpec.getDisplayLimit() == OutputLimitLimitType.SNAPSHOT && noDataWindowSingleStream);
        this.groupByRollupDesc = groupByRollupDesc;
        this.isJoin = isJoin;
        this.isHistoricalOnly = isHistoricalOnly;
    }

    public ResultSetProcessor instantiate(OrderByProcessor orderByProcessor, AggregationService aggregationService, AgentInstanceContext agentInstanceContext) {
        if (noDataWindowSingleSnapshot && !isHistoricalOnly) {
            return new ResultSetProcessorRowPerGroupRollupUnbound(this, orderByProcessor, aggregationService, agentInstanceContext);
        }
        return new ResultSetProcessorRowPerGroupRollup(this, orderByProcessor, aggregationService, agentInstanceContext);
    }

    public EventType getResultEventType()
    {
        return perLevelExpression.getSelectExprProcessor()[0].getResultEventType();
    }

    public boolean hasAggregation() {
        return true;
    }

    public ExprEvaluator[] getGroupKeyNodes() {
        return groupKeyNodes;
    }

    public ExprEvaluator getGroupKeyNode() {
        return groupKeyNode;
    }

    public boolean isSorting() {
        return isSorting;
    }

    public boolean isSelectRStream() {
        return isSelectRStream;
    }

    public boolean isUnidirectional() {
        return isUnidirectional;
    }

    public OutputLimitSpec getOutputLimitSpec() {
        return outputLimitSpec;
    }

    public ExprNode[] getGroupKeyNodeExpressions() {
        return groupKeyNodeExpressions;
    }

    public AggregationGroupByRollupDesc getGroupByRollupDesc() {
        return groupByRollupDesc;
    }

    public GroupByRollupPerLevelExpression getPerLevelExpression() {
        return perLevelExpression;
    }

    public boolean isJoin() {
        return isJoin;
    }

    public boolean isHistoricalOnly() {
        return isHistoricalOnly;
    }
}
