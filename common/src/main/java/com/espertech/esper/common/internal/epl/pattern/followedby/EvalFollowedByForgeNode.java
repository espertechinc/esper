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
package com.espertech.esper.common.internal.epl.pattern.followedby;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.compile.stage2.FilterSpecCompiled;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityPrint;
import com.espertech.esper.common.internal.epl.pattern.core.EvalFactoryNode;
import com.espertech.esper.common.internal.epl.pattern.core.EvalForgeNodeBase;
import com.espertech.esper.common.internal.epl.pattern.core.PatternExpressionPrecedenceEnum;
import com.espertech.esper.common.internal.epl.pattern.core.PatternExpressionUtil;
import com.espertech.esper.common.internal.schedule.ScheduleHandleCallbackProvider;

import java.io.StringWriter;
import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * This class represents a followed-by operator in the evaluation tree representing any event expressions.
 */
public class EvalFollowedByForgeNode extends EvalForgeNodeBase {
    private List<ExprNode> optionalMaxExpressions;

    public EvalFollowedByForgeNode(boolean attachPatternText, List<ExprNode> optionalMaxExpressions) {
        super(attachPatternText);
        this.optionalMaxExpressions = optionalMaxExpressions;
    }

    public List<ExprNode> getOptionalMaxExpressions() {
        return optionalMaxExpressions;
    }

    public void setOptionalMaxExpressions(List<ExprNode> optionalMaxExpressions) {
        this.optionalMaxExpressions = optionalMaxExpressions;
    }

    public final String toString() {
        return "EvalFollowedByNode children=" + this.getChildNodes().size();
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        if (optionalMaxExpressions == null || optionalMaxExpressions.isEmpty()) {
            PatternExpressionUtil.toPrecedenceFreeEPL(writer, "->", getChildNodes(), getPrecedence());
        } else {
            getChildNodes().get(0).toEPL(writer, PatternExpressionPrecedenceEnum.MINIMUM);
            for (int i = 1; i < getChildNodes().size(); i++) {
                ExprNode optionalMaxExpression = null;
                if (optionalMaxExpressions.size() > (i - 1)) {
                    optionalMaxExpression = optionalMaxExpressions.get(i - 1);
                }
                if (optionalMaxExpression == null) {
                    writer.append(" -> ");
                } else {
                    writer.append(" -[");
                    writer.append(ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(optionalMaxExpression));
                    writer.append("]> ");
                }
                getChildNodes().get(i).toEPL(writer, PatternExpressionPrecedenceEnum.MINIMUM);
            }
        }
    }

    public PatternExpressionPrecedenceEnum getPrecedence() {
        return PatternExpressionPrecedenceEnum.FOLLOWEDBY;
    }

    protected Class typeOfFactory() {
        return EvalFollowedByFactoryNode.class;
    }

    protected String nameOfFactory() {
        return "followedby";
    }

    protected void inlineCodegen(CodegenMethod method, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        method.getBlock().declareVar(EvalFactoryNode[].class, "children", newArrayByLength(EvalFactoryNode.class, constant(getChildNodes().size())));
        for (int i = 0; i < getChildNodes().size(); i++) {
            method.getBlock().assignArrayElement(ref("children"), constant(i), localMethod(getChildNodes().get(i).makeCodegen(method, symbols, classScope)));
        }
        method.getBlock()
                .exprDotMethod(ref("node"), "setChildren", ref("children"))
                .expression(exprDotMethodChain(symbols.getAddInitSvc(method)).add("addReadyCallback", ref("node")));

        if (optionalMaxExpressions != null && !optionalMaxExpressions.isEmpty()) {
            method.getBlock().declareVar(ExprEvaluator[].class, "evals", newArrayByLength(ExprEvaluator.class, constant(this.getChildNodes().size() - 1)));

            for (int i = 0; i < getChildNodes().size() - 1; i++) {
                if (optionalMaxExpressions.size() <= i) {
                    continue;
                }
                ExprNode optionalMaxExpression = optionalMaxExpressions.get(i);
                if (optionalMaxExpression == null) {
                    continue;
                }
                method.getBlock().assignArrayElement("evals", constant(i), ExprNodeUtilityCodegen.codegenEvaluatorNoCoerce(optionalMaxExpression.getForge(), method, this.getClass(), classScope));
            }
            method.getBlock().exprDotMethod(ref("node"), "setMaxPerChildEvals", ref("evals"));
        }
    }

    public void collectSelfFilterAndSchedule(List<FilterSpecCompiled> filters, List<ScheduleHandleCallbackProvider> schedules) {
    }
}
