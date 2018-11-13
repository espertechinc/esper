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
package com.espertech.esper.common.internal.epl.pattern.matchuntil;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.compile.stage2.FilterSpecCompiled;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityPrint;
import com.espertech.esper.common.internal.epl.pattern.core.EvalFactoryNode;
import com.espertech.esper.common.internal.epl.pattern.core.EvalForgeNodeBase;
import com.espertech.esper.common.internal.epl.pattern.core.MatchedEventConvertorForge;
import com.espertech.esper.common.internal.epl.pattern.core.PatternExpressionPrecedenceEnum;
import com.espertech.esper.common.internal.schedule.ScheduleHandleCallbackProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * This class represents a match-until observer in the evaluation tree representing any event expressions.
 */
public class EvalMatchUntilForgeNode extends EvalForgeNodeBase {
    private ExprNode lowerBounds;
    private ExprNode upperBounds;
    private ExprNode singleBound;
    private MatchedEventConvertorForge convertor;
    private int[] tagsArrayed;

    public EvalMatchUntilForgeNode(boolean attachPatternText, ExprNode lowerBounds, ExprNode upperBounds, ExprNode singleBound) {
        super(attachPatternText);
        if (singleBound != null && (lowerBounds != null || upperBounds != null)) {
            throw new IllegalArgumentException("Invalid bounds, specify either single bound or range bounds");
        }
        this.lowerBounds = lowerBounds;
        this.upperBounds = upperBounds;
        this.singleBound = singleBound;
    }

    protected Class typeOfFactory() {
        return EvalMatchUntilFactoryNode.class;
    }

    protected String nameOfFactory() {
        return "matchUntil";
    }

    protected void inlineCodegen(CodegenMethod method, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        method.getBlock().declareVar(EvalFactoryNode[].class, "children", newArrayByLength(EvalFactoryNode.class, constant(getChildNodes().size())));
        for (int i = 0; i < getChildNodes().size(); i++) {
            method.getBlock().assignArrayElement(ref("children"), constant(i), localMethod(getChildNodes().get(i).makeCodegen(method, symbols, classScope)));
        }
        CodegenExpressionRef node = ref("node");

        CodegenExpression converterExpression;
        if ((lowerBounds == null || lowerBounds.getForge().getForgeConstantType().isCompileTimeConstant()) &&
                (upperBounds == null || upperBounds.getForge().getForgeConstantType().isCompileTimeConstant()) &&
                (singleBound == null || singleBound.getForge().getForgeConstantType().isCompileTimeConstant())) {
            converterExpression = constantNull();
        } else {
            converterExpression = convertor.makeAnonymous(method, classScope);
        }
        method.getBlock()
                .exprDotMethod(node, "setChildren", ref("children"))
                .exprDotMethod(node, "setLowerBounds", lowerBounds == null ? constantNull() : ExprNodeUtilityCodegen.codegenEvaluator(lowerBounds.getForge(), method, this.getClass(), classScope))
                .exprDotMethod(node, "setUpperBounds", upperBounds == null ? constantNull() : ExprNodeUtilityCodegen.codegenEvaluator(upperBounds.getForge(), method, this.getClass(), classScope))
                .exprDotMethod(node, "setSingleBound", singleBound == null ? constantNull() : ExprNodeUtilityCodegen.codegenEvaluator(singleBound.getForge(), method, this.getClass(), classScope))
                .exprDotMethod(node, "setTagsArrayed", constant(tagsArrayed))
                .exprDotMethod(node, "setOptionalConvertor", converterExpression);
    }

    public void collectSelfFilterAndSchedule(List<FilterSpecCompiled> filters, List<ScheduleHandleCallbackProvider> schedules) {
        // nothing for this node, children navigated elsewhere
    }

    /**
     * Returns an array of tags for events, which is all tags used within the repeat-operator.
     *
     * @return array of tags
     */
    public int[] getTagsArrayed() {
        return tagsArrayed;
    }

    /**
     * Sets the convertor for matching events to events-per-stream.
     *
     * @param convertor convertor
     */
    public void setConvertor(MatchedEventConvertorForge convertor) {
        this.convertor = convertor;
    }

    public ExprNode getLowerBounds() {
        return lowerBounds;
    }

    public ExprNode getUpperBounds() {
        return upperBounds;
    }

    public ExprNode getSingleBound() {
        return singleBound;
    }

    public void setLowerBounds(ExprNode lowerBounds) {
        this.lowerBounds = lowerBounds;
    }

    public void setUpperBounds(ExprNode upperBounds) {
        this.upperBounds = upperBounds;
    }

    public void setSingleBound(ExprNode singleBound) {
        this.singleBound = singleBound;
    }

    /**
     * Sets the tags used within the repeat operator.
     *
     * @param tagsArrayedSet tags used within the repeat operator
     */
    public void setTagsArrayedSet(int[] tagsArrayedSet) {
        tagsArrayed = tagsArrayedSet;
    }

    public MatchedEventConvertorForge getConvertor() {
        return convertor;
    }

    public final String toString() {
        return "EvalMatchUntilNode children=" + this.getChildNodes().size();
    }

    public boolean isFilterChildNonQuitting() {
        return true;
    }

    public boolean isStateful() {
        return true;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        if (singleBound != null) {
            writer.append("[");
            writer.append(ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(singleBound));
            writer.append("] ");
        } else {
            if (lowerBounds != null || upperBounds != null) {
                writer.append("[");
                if (lowerBounds != null) {
                    writer.append(ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(lowerBounds));
                }
                writer.append(":");
                if (upperBounds != null) {
                    writer.append(ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(upperBounds));
                }
                writer.append("] ");
            }
        }
        getChildNodes().get(0).toEPL(writer, getPrecedence());
        if (getChildNodes().size() > 1) {
            writer.append(" until ");
            getChildNodes().get(1).toEPL(writer, getPrecedence());
        }
    }

    public PatternExpressionPrecedenceEnum getPrecedence() {
        return PatternExpressionPrecedenceEnum.REPEAT_UNTIL;
    }

    private static final Logger log = LoggerFactory.getLogger(EvalMatchUntilForgeNode.class);
}
