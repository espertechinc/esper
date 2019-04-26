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

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyClassRef;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyCodegen;
import com.espertech.esper.common.internal.compile.stage2.FilterSpecCompiled;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityPrint;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityQuery;
import com.espertech.esper.common.internal.epl.expression.time.eval.TimePeriodComputeForge;
import com.espertech.esper.common.internal.epl.pattern.core.EvalForgeNodeBase;
import com.espertech.esper.common.internal.epl.pattern.core.MatchedEventConvertorForge;
import com.espertech.esper.common.internal.epl.pattern.core.PatternExpressionPrecedenceEnum;
import com.espertech.esper.common.internal.schedule.ScheduleHandleCallbackProvider;

import java.io.StringWriter;
import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * This class represents an 'every-distinct' operator in the evaluation tree representing an event expression.
 */
public class EvalEveryDistinctForgeNode extends EvalForgeNodeBase {
    private List<ExprNode> expressions;
    private transient MatchedEventConvertorForge convertor;
    private TimePeriodComputeForge timePeriodComputeForge;
    private ExprNode expiryTimeExp;
    private List<ExprNode> distinctExpressions;
    private MultiKeyClassRef distinctMultiKey;

    /**
     * Ctor.
     *
     * @param expressions       distinct-value expressions
     * @param attachPatternText whether to attach EPL subexpression text
     */
    public EvalEveryDistinctForgeNode(boolean attachPatternText, List<ExprNode> expressions) {
        super(attachPatternText);
        this.expressions = expressions;
    }

    protected Class typeOfFactory() {
        return EvalEveryDistinctFactoryNode.class;
    }

    protected String nameOfFactory() {
        return "everyDistinct";
    }

    protected void inlineCodegen(CodegenMethod method, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenExpression distinctEval = MultiKeyCodegen.codegenExprEvaluatorMayMultikey(distinctExpressions.toArray(new ExprNode[0]),
            null, distinctMultiKey, method, classScope);
        method.getBlock()
            .exprDotMethod(ref("node"), "setChildNode", localMethod(getChildNodes().get(0).makeCodegen(method, symbols, classScope)))
            .exprDotMethod(ref("node"), "setDistinctExpression", distinctEval)
            .exprDotMethod(ref("node"), "setDistinctTypes", constant(ExprNodeUtilityQuery.getExprResultTypes(distinctExpressions)))
            .exprDotMethod(ref("node"), "setDistinctSerde", distinctMultiKey.getExprMKSerde(method, classScope))
            .exprDotMethod(ref("node"), "setConvertor", convertor.makeAnonymous(method, classScope))
            .exprDotMethod(ref("node"), "setTimePeriodCompute", timePeriodComputeForge == null ? constantNull() : timePeriodComputeForge.makeEvaluator(method, classScope));
    }

    public void collectSelfFilterAndSchedule(List<FilterSpecCompiled> filters, List<ScheduleHandleCallbackProvider> schedules) {
        // nothing to collect for this node
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
    public void setConvertor(MatchedEventConvertorForge convertor) {
        this.convertor = convertor;
    }

    public void setDistinctExpressions(List<ExprNode> distinctExpressions, MultiKeyClassRef distincMultiKey, TimePeriodComputeForge timePeriodComputeForge, ExprNode expiryTimeExp) {
        this.distinctExpressions = distinctExpressions;
        this.distinctMultiKey = distincMultiKey;
        this.timePeriodComputeForge = timePeriodComputeForge;
        this.expiryTimeExp = expiryTimeExp;
    }

    public boolean isFilterChildNonQuitting() {
        return true;
    }

    public boolean isStateful() {
        return true;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.append("every-distinct(");
        ExprNodeUtilityPrint.toExpressionStringParameterList(distinctExpressions, writer);
        if (expiryTimeExp != null) {
            writer.append(",");
            writer.append(ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(expiryTimeExp));
        }
        writer.append(") ");
        this.getChildNodes().get(0).toEPL(writer, getPrecedence());
    }

    public PatternExpressionPrecedenceEnum getPrecedence() {
        return PatternExpressionPrecedenceEnum.UNARY;
    }
}
