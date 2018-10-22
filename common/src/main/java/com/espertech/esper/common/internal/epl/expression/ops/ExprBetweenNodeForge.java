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
package com.espertech.esper.common.internal.epl.expression.ops;


import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationBuilderExpr;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constantFalse;

public class ExprBetweenNodeForge implements ExprForgeInstrumentable {

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
                    return false;
                }

            };
        }
        ExprNode[] nodes = parent.getChildNodes();
        return new ExprBetweenNodeForgeEval(this, nodes[0].getForge().getExprEvaluator(), nodes[1].getForge().getExprEvaluator(), nodes[2].getForge().getExprEvaluator());
    }

    public ExprForgeConstantType getForgeConstantType() {
        return ExprForgeConstantType.NONCONST;
    }

    public CodegenExpression evaluateCodegenUninstrumented(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        if (isAlwaysFalse) {
            return constantFalse();
        }
        return ExprBetweenNodeForgeEval.codegen(this, codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return new InstrumentationBuilderExpr(this.getClass(), this, "ExprBetween", requiredType, codegenMethodScope, exprSymbol, codegenClassScope).build();
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
