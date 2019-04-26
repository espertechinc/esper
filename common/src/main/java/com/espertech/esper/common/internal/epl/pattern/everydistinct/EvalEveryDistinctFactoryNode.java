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
package com.espertech.esper.common.internal.epl.pattern.everydistinct;

import com.espertech.esper.common.client.serde.DataInputOutputSerde;
import com.espertech.esper.common.internal.compile.stage2.EvalNodeUtil;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.time.eval.TimePeriodCompute;
import com.espertech.esper.common.internal.epl.pattern.core.*;

/**
 * This class represents an 'every-distinct' operator in the evaluation tree representing an event expression.
 */
public class EvalEveryDistinctFactoryNode extends EvalFactoryNodeBase {
    private ExprEvaluator distinctExpression;
    private MatchedEventConvertor convertor;
    private TimePeriodCompute timePeriodCompute;
    protected EvalFactoryNode childNode;
    private Class[] distinctTypes;
    private DataInputOutputSerde<Object> distinctSerde;

    public void setDistinctExpression(ExprEvaluator distinctExpression) {
        this.distinctExpression = distinctExpression;
    }

    public void setConvertor(MatchedEventConvertor convertor) {
        this.convertor = convertor;
    }

    public void setTimePeriodCompute(TimePeriodCompute timePeriodCompute) {
        this.timePeriodCompute = timePeriodCompute;
    }

    public void setChildNode(EvalFactoryNode childNode) {
        this.childNode = childNode;
    }

    public void setDistinctTypes(Class[] distinctTypes) {
        this.distinctTypes = distinctTypes;
    }

    public EvalNode makeEvalNode(PatternAgentInstanceContext agentInstanceContext, EvalNode parentNode) {
        EvalNode child = EvalNodeUtil.makeEvalNodeSingleChild(childNode, agentInstanceContext, parentNode);
        return new EvalEveryDistinctNode(this, child, agentInstanceContext);
    }

    public ExprEvaluator getDistinctExpression() {
        return distinctExpression;
    }

    public MatchedEventConvertor getConvertor() {
        return convertor;
    }

    public boolean isFilterChildNonQuitting() {
        return true;
    }

    public boolean isStateful() {
        return true;
    }

    public long absExpiry(PatternAgentInstanceContext context) {
        long current = context.getStatementContext().getSchedulingService().getTime();
        return current + timePeriodCompute.deltaAdd(current, null, true, context.getAgentInstanceContext());
    }

    public TimePeriodCompute getTimePeriodCompute() {
        return timePeriodCompute;
    }

    public EvalFactoryNode getChildNode() {
        return childNode;
    }

    public Class[] getDistinctTypes() {
        return distinctTypes;
    }

    public DataInputOutputSerde<Object> getDistinctSerde() {
        return distinctSerde;
    }

    public void setDistinctSerde(DataInputOutputSerde<Object> distinctSerde) {
        this.distinctSerde = distinctSerde;
    }

    public void accept(EvalFactoryNodeVisitor visitor) {
        visitor.visit(this);
        childNode.accept(visitor);
    }
}
