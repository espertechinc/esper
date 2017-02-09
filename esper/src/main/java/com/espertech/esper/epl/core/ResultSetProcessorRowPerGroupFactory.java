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
 * Result set processor prototype for the fully-grouped case:
 * there is a group-by and all non-aggregation event properties in the select clause are listed in the group by,
 * and there are aggregation functions.
 */
public class ResultSetProcessorRowPerGroupFactory implements ResultSetProcessorFactory {
    private final SelectExprProcessor selectExprProcessor;
    private final ExprNode[] groupKeyNodeExpressions;
    private final ExprEvaluator groupKeyNode;
    private final ExprEvaluator[] groupKeyNodes;
    private final ExprEvaluator optionalHavingNode;
    private final boolean isSorting;
    private final boolean isSelectRStream;
    private final boolean isUnidirectional;
    private final OutputLimitSpec outputLimitSpec;
    private final boolean noDataWindowSingleSnapshot;
    private final boolean isHistoricalOnly;
    private final ResultSetProcessorHelperFactory resultSetProcessorHelperFactory;
    private final boolean enableOutputLimitOpt;
    private final int numStreams;
    private final OutputConditionPolledFactory optionalOutputFirstConditionFactory;

    public ResultSetProcessorRowPerGroupFactory(SelectExprProcessor selectExprProcessor,
                                                ExprNode[] groupKeyNodeExpressions,
                                                ExprEvaluator[] groupKeyNodes,
                                                ExprEvaluator optionalHavingNode,
                                                boolean isSelectRStream,
                                                boolean isUnidirectional,
                                                OutputLimitSpec outputLimitSpec,
                                                boolean isSorting,
                                                boolean noDataWindowSingleStream,
                                                boolean isHistoricalOnly,
                                                boolean iterateUnbounded,
                                                ResultSetProcessorHelperFactory resultSetProcessorHelperFactory,
                                                boolean enableOutputLimitOpt,
                                                int numStreams,
                                                OutputConditionPolledFactory optionalOutputFirstConditionFactory) {
        this.groupKeyNodeExpressions = groupKeyNodeExpressions;
        this.selectExprProcessor = selectExprProcessor;
        this.groupKeyNodes = groupKeyNodes;
        if (groupKeyNodes.length == 1) {
            this.groupKeyNode = groupKeyNodes[0];
        } else {
            this.groupKeyNode = null;
        }
        this.optionalHavingNode = optionalHavingNode;
        this.isSorting = isSorting;
        this.isSelectRStream = isSelectRStream;
        this.isUnidirectional = isUnidirectional;
        this.outputLimitSpec = outputLimitSpec;
        this.noDataWindowSingleSnapshot = iterateUnbounded || (outputLimitSpec != null && outputLimitSpec.getDisplayLimit() == OutputLimitLimitType.SNAPSHOT && noDataWindowSingleStream);
        this.isHistoricalOnly = isHistoricalOnly;
        this.resultSetProcessorHelperFactory = resultSetProcessorHelperFactory;
        this.enableOutputLimitOpt = enableOutputLimitOpt;
        this.numStreams = numStreams;
        this.optionalOutputFirstConditionFactory = optionalOutputFirstConditionFactory;
    }

    public ResultSetProcessor instantiate(OrderByProcessor orderByProcessor, AggregationService aggregationService, AgentInstanceContext agentInstanceContext) {
        if (noDataWindowSingleSnapshot && !isHistoricalOnly) {
            return new ResultSetProcessorRowPerGroupUnbound(this, selectExprProcessor, orderByProcessor, aggregationService, agentInstanceContext);
        }
        return new ResultSetProcessorRowPerGroup(this, selectExprProcessor, orderByProcessor, aggregationService, agentInstanceContext);
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

    public ExprEvaluator getGroupKeyNode() {
        return groupKeyNode;
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

    public ExprNode[] getGroupKeyNodeExpressions() {
        return groupKeyNodeExpressions;
    }

    public boolean isHistoricalOnly() {
        return isHistoricalOnly;
    }

    public ResultSetProcessorType getResultSetProcessorType() {
        return ResultSetProcessorType.FULLYAGGREGATED_GROUPED;
    }

    public boolean isOutputLast() {
        return outputLimitSpec != null && outputLimitSpec.getDisplayLimit() == OutputLimitLimitType.LAST;
    }

    public boolean isOutputAll() {
        return outputLimitSpec != null && outputLimitSpec.getDisplayLimit() == OutputLimitLimitType.ALL;
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

    public OutputConditionPolledFactory getOptionalOutputFirstConditionFactory() {
        return optionalOutputFirstConditionFactory;
    }

    public ResultSetProcessorHelperFactory getResultSetProcessorHelperFactory() {
        return resultSetProcessorHelperFactory;
    }
}
