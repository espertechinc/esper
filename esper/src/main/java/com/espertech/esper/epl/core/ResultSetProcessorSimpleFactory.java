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
 * Result set processor prototype for the simplest case: no aggregation functions used in the select clause, and no group-by.
 */
public class ResultSetProcessorSimpleFactory implements ResultSetProcessorFactory {
    private final boolean isSelectRStream;
    private final SelectExprProcessor selectExprProcessor;
    private final ExprEvaluator optionalHavingExpr;
    private final OutputLimitSpec outputLimitSpec;
    private final boolean enableOutputLimitOpt;
    private final ResultSetProcessorHelperFactory resultSetProcessorHelperFactory;
    private final int numStreams;

    public ResultSetProcessorSimpleFactory(SelectExprProcessor selectExprProcessor,
                                           ExprEvaluator optionalHavingNode,
                                           boolean isSelectRStream,
                                           OutputLimitSpec outputLimitSpec,
                                           boolean enableOutputLimitOpt,
                                           ResultSetProcessorHelperFactory resultSetProcessorHelperFactory,
                                           int numStreams) {
        this.selectExprProcessor = selectExprProcessor;
        this.optionalHavingExpr = optionalHavingNode;
        this.isSelectRStream = isSelectRStream;
        this.outputLimitSpec = outputLimitSpec;
        this.enableOutputLimitOpt = enableOutputLimitOpt;
        this.resultSetProcessorHelperFactory = resultSetProcessorHelperFactory;
        this.numStreams = numStreams;
    }

    public ResultSetProcessorType getResultSetProcessorType() {
        return ResultSetProcessorType.UNAGGREGATED_UNGROUPED;
    }

    public ResultSetProcessor instantiate(OrderByProcessor orderByProcessor, AggregationService aggregationService, AgentInstanceContext agentInstanceContext) {
        return new ResultSetProcessorSimple(this, selectExprProcessor, orderByProcessor, agentInstanceContext);
    }

    public EventType getResultEventType() {
        return selectExprProcessor.getResultEventType();
    }

    public boolean hasAggregation() {
        return false;
    }

    public boolean isSelectRStream() {
        return isSelectRStream;
    }

    public ExprEvaluator getOptionalHavingExpr() {
        return optionalHavingExpr;
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

    public ResultSetProcessorHelperFactory getResultSetProcessorHelperFactory() {
        return resultSetProcessorHelperFactory;
    }

    public int getNumStreams() {
        return numStreams;
    }
}
