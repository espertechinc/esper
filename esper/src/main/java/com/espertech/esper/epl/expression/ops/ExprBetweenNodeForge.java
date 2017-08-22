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
package com.espertech.esper.epl.expression.ops;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.constantFalse;

public class ExprBetweenNodeForge implements ExprForge {

    private final ExprBetweenNodeImpl parent;
    private final ExprBetweenNodeImpl.ExprBetweenComp computer;
    private final boolean isAlwaysFalse;

    public ExprBetweenNodeForge(ExprBetweenNodeImpl parent, ExprBetweenNodeImpl.ExprBetweenComp computer, boolean isAlwaysFalse) {
        this.parent = parent;
        this.computer = computer;
        this.isAlwaysFalse = isAlwaysFalse;
    }

    public ExprEvaluator getExprEvaluator() {
        if (isAlwaysFalse) {
            return new ExprEvaluator() {
                public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
                    if (InstrumentationHelper.ENABLED) {
                        InstrumentationHelper.get().qExprBetween(parent);
                        InstrumentationHelper.get().aExprBetween(false);
                    }
                    return false;
                }

            };
        }
        ExprNode[] nodes = parent.getChildNodes();
        return new ExprBetweenNodeForgeEval(this, nodes[0].getForge().getExprEvaluator(), nodes[1].getForge().getExprEvaluator(), nodes[2].getForge().getExprEvaluator());
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        if (isAlwaysFalse) {
            return constantFalse();
        }
        return ExprBetweenNodeForgeEval.codegen(this, codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public ExprForgeComplexityEnum getComplexity() {
        return isAlwaysFalse ? ExprForgeComplexityEnum.NONE : ExprForgeComplexityEnum.INTER;
    }

    public Class getEvaluationType() {
        return Boolean.class;
    }

    public ExprBetweenNodeImpl getForgeRenderable() {
        return parent;
    }

    public ExprBetweenNodeImpl.ExprBetweenComp getComputer() {
        return computer;
    }

    public boolean isAlwaysFalse() {
        return isAlwaysFalse;
    }
}
