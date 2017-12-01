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
package com.espertech.esper.pattern;

import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.time.ExprTimePeriodEvalDeltaConst;
import com.espertech.esper.epl.util.ExprNodeUtilityRich;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.List;

/**
 * This class represents an 'every-distinct' operator in the evaluation tree representing an event expression.
 */
public class EvalEveryDistinctFactoryNode extends EvalNodeFactoryBase {
    protected List<ExprNode> expressions;
    protected transient ExprEvaluator[] distinctExpressionsArray;
    private transient MatchedEventConvertor convertor;
    private ExprTimePeriodEvalDeltaConst timeDeltaComputation;
    private ExprNode expiryTimeExp;
    protected List<ExprNode> distinctExpressions;
    private static final long serialVersionUID = 7455570958072753956L;

    /**
     * Ctor.
     *
     * @param expressions distinct-value expressions
     */
    protected EvalEveryDistinctFactoryNode(List<ExprNode> expressions) {
        this.expressions = expressions;
    }

    public EvalNode makeEvalNode(PatternAgentInstanceContext agentInstanceContext, EvalNode parentNode) {
        EvalNode child = EvalNodeUtil.makeEvalNodeSingleChild(this.getChildNodes(), agentInstanceContext, parentNode);
        return new EvalEveryDistinctNode(this, child, agentInstanceContext);
    }

    public ExprEvaluator[] getDistinctExpressionsArray() {
        return distinctExpressionsArray;
    }

    public MatchedEventConvertor getConvertor() {
        return convertor;
    }

    public final String toString() {
        return "EvalEveryNode children=" + this.getChildNodes().size();
    }

    /**
     * Returns all expressions.
     *
     * @return expressions
     */
    public List<ExprNode> getExpressions() {
        return expressions;
    }

    /**
     * Returns distinct expressions.
     *
     * @return expressions
     */
    public List<ExprNode> getDistinctExpressions() {
        return distinctExpressions;
    }

    /**
     * Sets the convertor for matching events to events-per-stream.
     *
     * @param convertor convertor
     */
    public void setConvertor(MatchedEventConvertor convertor) {
        this.convertor = convertor;
    }

    public void setDistinctExpressions(List<ExprNode> distinctExpressions, ExprTimePeriodEvalDeltaConst timeDeltaComputation, ExprNode expiryTimeExp, EngineImportService engineImportService, String statementName) {
        this.distinctExpressions = distinctExpressions;
        this.timeDeltaComputation = timeDeltaComputation;
        this.expiryTimeExp = expiryTimeExp;
        this.distinctExpressionsArray = ExprNodeUtilityRich.getEvaluatorsMayCompile(distinctExpressions, engineImportService, this.getClass(), false, statementName);
    }

    public boolean isFilterChildNonQuitting() {
        return true;
    }

    public boolean isStateful() {
        return true;
    }

    public long absExpiry(PatternAgentInstanceContext context) {
        long current = context.getStatementContext().getSchedulingService().getTime();
        return current + timeDeltaComputation.deltaAdd(current);
    }

    public ExprTimePeriodEvalDeltaConst getTimeDeltaComputation() {
        return timeDeltaComputation;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.append("every-distinct(");
        ExprNodeUtilityCore.toExpressionStringParameterList(distinctExpressions, writer);
        if (expiryTimeExp != null) {
            writer.append(",");
            writer.append(ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(expiryTimeExp));
        }
        writer.append(") ");
        this.getChildNodes().get(0).toEPL(writer, getPrecedence());
    }

    public PatternExpressionPrecedenceEnum getPrecedence() {
        return PatternExpressionPrecedenceEnum.UNARY;
    }

    private static final Logger log = LoggerFactory.getLogger(EvalEveryNode.class);
}
