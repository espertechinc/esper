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
import com.espertech.esper.epl.spec.OutputLimitLimitType;
import com.espertech.esper.epl.spec.OutputLimitSpec;

/**
 * Result set processor prototype for the case: aggregation functions used in the select clause, and no group-by,
 * and not all of the properties in the select clause are under an aggregation function.
 */
public class ResultSetProcessorAggregateAllFactory implements ResultSetProcessorFactory {
    private final SelectExprProcessor selectExprProcessor;
    private final ExprEvaluator optionalHavingNode;
    private final boolean isSelectRStream;
    private final boolean isUnidirectional;
    private final boolean isHistoricalOnly;
    private final OutputLimitSpec outputLimitSpec;
    private final boolean enableOutputLimitOpt;
    private final ResultSetProcessorHelperFactory resultSetProcessorHelperFactory;

    public ResultSetProcessorAggregateAllFactory(SelectExprProcessor selectExprProcessor,
                                                 ExprEvaluator optionalHavingNode,
                                                 boolean isSelectRStream,
                                                 boolean isUnidirectional,
                                                 boolean isHistoricalOnly,
                                                 OutputLimitSpec outputLimitSpec,
                                                 boolean enableOutputLimitOpt,
                                                 ResultSetProcessorHelperFactory resultSetProcessorHelperFactory) {
        this.selectExprProcessor = selectExprProcessor;
        this.optionalHavingNode = optionalHavingNode;
        this.isSelectRStream = isSelectRStream;
        this.isUnidirectional = isUnidirectional;
        this.isHistoricalOnly = isHistoricalOnly;
        this.outputLimitSpec = outputLimitSpec;
        this.enableOutputLimitOpt = enableOutputLimitOpt;
        this.resultSetProcessorHelperFactory = resultSetProcessorHelperFactory;
    }

    public ResultSetProcessor instantiate(OrderByProcessor orderByProcessor, AggregationService aggregationService, AgentInstanceContext agentInstanceContext) {
        return new ResultSetProcessorAggregateAll(this, selectExprProcessor, orderByProcessor, aggregationService, agentInstanceContext);
    }

    public EventType getResultEventType() {
        return selectExprProcessor.getResultEventType();
    }

    public boolean hasAggregation() {
        return true;
    }

    public ExprEvaluator getOptionalHavingNode() {
        return optionalHavingNode;
    }

    public boolean isSelectRStream() {
        return isSelectRStream;
    }

    public boolean isUnidirectional() {
        return isUnidirectional;
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

    public boolean isEnableOutputLimitOpt() {
        return enableOutputLimitOpt;
    }

    public ResultSetProcessorType getResultSetProcessorType() {
        return ResultSetProcessorType.AGGREGATED_UNGROUPED;
    }

    public ResultSetProcessorHelperFactory getResultSetProcessorHelperFactory() {
        return resultSetProcessorHelperFactory;
    }
}
