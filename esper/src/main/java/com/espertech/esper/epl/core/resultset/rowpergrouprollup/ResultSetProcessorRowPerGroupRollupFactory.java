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
package com.espertech.esper.epl.core.resultset.rowpergrouprollup;

import com.espertech.esper.client.EventType;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.agg.rollup.GroupByRollupPerLevelExpression;
import com.espertech.esper.epl.agg.service.common.AggregationGroupByRollupDesc;
import com.espertech.esper.epl.agg.service.common.AggregationService;
import com.espertech.esper.epl.core.orderby.OrderByProcessor;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessor;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessorFactory;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessorHelperFactory;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessorOutputConditionType;
import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.spec.OutputLimitSpec;
import com.espertech.esper.epl.view.OutputConditionPolledFactory;

/**
 * Result set processor prototype for the fully-grouped case:
 * there is a group-by and all non-aggregation event properties in the select clause are listed in the group by,
 * and there are aggregation functions.
 */
public class ResultSetProcessorRowPerGroupRollupFactory implements ResultSetProcessorFactory {
    private final EventType resultEventType;
    private final GroupByRollupPerLevelExpression perLevelExpression;
    private final ExprNode[] groupKeyNodeExpressions;
    private final ExprEvaluator groupKeyNode;
    private final ExprEvaluator[] groupKeyNodes;
    private final boolean isSorting;
    private final boolean isSelectRStream;
    private final boolean isUnidirectional;
    private final OutputLimitSpec outputLimitSpec;
    private final boolean unbounded;
    private final AggregationGroupByRollupDesc groupByRollupDesc;
    private final boolean isJoin;
    private final boolean isHistoricalOnly;
    private final OutputConditionPolledFactory optionalOutputFirstConditionFactory;
    private final ResultSetProcessorHelperFactory resultSetProcessorHelperFactory;
    private final ResultSetProcessorOutputConditionType outputConditionType;
    private final int numStreams;
    private final Class[] groupKeyTypes;

    public ResultSetProcessorRowPerGroupRollupFactory(EventType resultEventType,
                                                      GroupByRollupPerLevelExpression perLevelExpression,
                                                      ExprNode[] groupKeyNodeExpressions,
                                                      ExprEvaluator[] groupKeyNodes,
                                                      boolean isSelectRStream,
                                                      boolean isUnidirectional,
                                                      OutputLimitSpec outputLimitSpec,
                                                      boolean isSorting,
                                                      AggregationGroupByRollupDesc groupByRollupDesc,
                                                      boolean isJoin,
                                                      boolean isHistoricalOnly,
                                                      OutputConditionPolledFactory optionalOutputFirstConditionFactory,
                                                      ResultSetProcessorHelperFactory resultSetProcessorHelperFactory,
                                                      ResultSetProcessorOutputConditionType outputConditionType,
                                                      int numStreams,
                                                      boolean unbounded) {
        this.resultEventType = resultEventType;
        this.groupKeyNodeExpressions = groupKeyNodeExpressions;
        this.perLevelExpression = perLevelExpression;
        this.groupKeyNodes = groupKeyNodes;
        if (groupKeyNodes.length == 1) {
            this.groupKeyNode = groupKeyNodes[0];
        } else {
            this.groupKeyNode = null;
        }
        this.isSorting = isSorting;
        this.isSelectRStream = isSelectRStream;
        this.isUnidirectional = isUnidirectional;
        this.outputLimitSpec = outputLimitSpec;
        this.groupByRollupDesc = groupByRollupDesc;
        this.isJoin = isJoin;
        this.isHistoricalOnly = isHistoricalOnly;
        this.optionalOutputFirstConditionFactory = optionalOutputFirstConditionFactory;
        this.resultSetProcessorHelperFactory = resultSetProcessorHelperFactory;
        this.outputConditionType = outputConditionType;
        this.numStreams = numStreams;
        this.groupKeyTypes = ExprNodeUtilityCore.getExprResultTypes(groupKeyNodeExpressions);
        this.unbounded = unbounded;
    }

    public ResultSetProcessor instantiate(OrderByProcessor orderByProcessor, AggregationService aggregationService, AgentInstanceContext agentInstanceContext) {
        if (unbounded) {
            return new ResultSetProcessorRowPerGroupRollupUnbound(ResultSetProcessorRowPerGroupRollupFactory.this, orderByProcessor, aggregationService, agentInstanceContext);
        }
        return new ResultSetProcessorRowPerGroupRollupImpl(ResultSetProcessorRowPerGroupRollupFactory.this, orderByProcessor, aggregationService, agentInstanceContext);
    }

    public EventType getResultEventType() {
        return resultEventType;
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

    public OutputConditionPolledFactory getOptionalOutputFirstConditionFactory() {
        return optionalOutputFirstConditionFactory;
    }

    public ResultSetProcessorOutputConditionType getOutputConditionType() {
        return outputConditionType;
    }

    public ResultSetProcessorHelperFactory getResultSetProcessorHelperFactory() {
        return resultSetProcessorHelperFactory;
    }

    public int getNumStreams() {
        return numStreams;
    }

    public Class[] getGroupKeyTypes() {
        return groupKeyTypes;
    }
}
