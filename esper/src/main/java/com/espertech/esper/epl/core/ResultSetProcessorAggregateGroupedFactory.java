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

import com.espertech.esper.client.EventType;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.agg.service.AggregationService;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.spec.OutputLimitLimitType;
import com.espertech.esper.epl.spec.OutputLimitSpec;
import com.espertech.esper.epl.view.OutputConditionPolledFactory;

/**
 * Result-set processor prototype for the aggregate-grouped case:
 * there is a group-by and one or more non-aggregation event properties in the select clause are not listed in the group by,
 * and there are aggregation functions.
 */
public class ResultSetProcessorAggregateGroupedFactory implements ResultSetProcessorFactory {
    private final SelectExprProcessor selectExprProcessor;
    private final ExprNode[] groupKeyNodeExpressions;
    private final ExprEvaluator groupKeyNode;
    private final ExprEvaluator[] groupKeyNodes;
    private final ExprEvaluator optionalHavingNode;
    private final boolean isSorting;
    private final boolean isSelectRStream;
    private final boolean isUnidirectional;
    private final OutputLimitSpec outputLimitSpec;
    private final boolean isHistoricalOnly;
    private final ResultSetProcessorHelperFactory resultSetProcessorHelperFactory;
    private final OutputConditionPolledFactory optionalOutputFirstConditionFactory;
    private final boolean enableOutputLimitOpt;
    private final int numStreams;

    public ResultSetProcessorAggregateGroupedFactory(SelectExprProcessor selectExprProcessor,
                                                     ExprNode[] groupKeyNodeExpressions,
                                                     ExprEvaluator[] groupKeyNodes,
                                                     ExprEvaluator optionalHavingNode,
                                                     boolean isSelectRStream,
                                                     boolean isUnidirectional,
                                                     OutputLimitSpec outputLimitSpec,
                                                     boolean isSorting,
                                                     boolean isHistoricalOnly,
                                                     ResultSetProcessorHelperFactory resultSetProcessorHelperFactory,
                                                     OutputConditionPolledFactory optionalOutputFirstConditionFactory,
                                                     boolean enableOutputLimitOpt,
                                                     int numStreams) {
        this.selectExprProcessor = selectExprProcessor;
        this.groupKeyNodeExpressions = groupKeyNodeExpressions;
        if (groupKeyNodes.length == 1) {
            groupKeyNode = groupKeyNodes[0];
        } else {
            groupKeyNode = null;
        }
        this.groupKeyNodes = groupKeyNodes;
        this.optionalHavingNode = optionalHavingNode;
        this.isSorting = isSorting;
        this.isSelectRStream = isSelectRStream;
        this.isUnidirectional = isUnidirectional;
        this.outputLimitSpec = outputLimitSpec;
        this.isHistoricalOnly = isHistoricalOnly;
        this.resultSetProcessorHelperFactory = resultSetProcessorHelperFactory;
        this.optionalOutputFirstConditionFactory = optionalOutputFirstConditionFactory;
        this.enableOutputLimitOpt = enableOutputLimitOpt;
        this.numStreams = numStreams;
    }

    public ResultSetProcessor instantiate(OrderByProcessor orderByProcessor, AggregationService aggregationService, AgentInstanceContext agentInstanceContext) {
        return new ResultSetProcessorAggregateGrouped(this, selectExprProcessor, orderByProcessor, aggregationService, agentInstanceContext);
    }

    public EventType getResultEventType() {
        return selectExprProcessor.getResultEventType();
    }

    public boolean hasAggregation() {
        return true;
    }

    public ExprEvaluator[] getGroupKeyNodes() {
        return groupKeyNodes;
    }

    public ExprEvaluator getOptionalHavingNode() {
        return optionalHavingNode;
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

    public ExprEvaluator getGroupKeyNode() {
        return groupKeyNode;
    }

    public ExprNode[] getGroupKeyNodeExpressions() {
        return groupKeyNodeExpressions;
    }

    public boolean isHistoricalOnly() {
        return isHistoricalOnly;
    }

    public boolean isOutputLast() {
        return outputLimitSpec != null && outputLimitSpec.getDisplayLimit() == OutputLimitLimitType.LAST;
    }

    public boolean isOutputAll() {
        return outputLimitSpec != null && outputLimitSpec.getDisplayLimit() == OutputLimitLimitType.ALL;
    }

    public ResultSetProcessorType getResultSetProcessorType() {
        return ResultSetProcessorType.AGGREGATED_GROUPED;
    }

    public OutputConditionPolledFactory getOptionalOutputFirstConditionFactory() {
        return optionalOutputFirstConditionFactory;
    }

    public boolean isEnableOutputLimitOpt() {
        return enableOutputLimitOpt;
    }

    public int getNumStreams() {
        return numStreams;
    }

    public boolean isOutputFirst() {
        return outputLimitSpec != null && outputLimitSpec.getDisplayLimit() == OutputLimitLimitType.FIRST;
    }

    public ResultSetProcessorHelperFactory getResultSetProcessorHelperFactory() {
        return resultSetProcessorHelperFactory;
    }
}
