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

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprForgeConstantType;
import com.espertech.esper.common.internal.epl.expression.core.ExprForgeInstrumentable;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityQuery;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationBuilderExpr;
import com.espertech.esper.common.internal.type.RelationalOpEnum;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;

public class ExprRelationalOpAllAnyNodeForge implements ExprForgeInstrumentable {
    private final ExprRelationalOpAllAnyNode parent;
    private final RelationalOpEnum.Computer computer;
    private final boolean hasCollectionOrArray;

    public ExprRelationalOpAllAnyNodeForge(ExprRelationalOpAllAnyNode parent, RelationalOpEnum.Computer computer, boolean hasCollectionOrArray) {
        this.parent = parent;
        this.computer = computer;
        this.hasCollectionOrArray = hasCollectionOrArray;
    }

    public ExprEvaluator getExprEvaluator() {
        return new ExprRelationalOpAllAnyNodeForgeEval(this, ExprNodeUtilityQuery.getEvaluatorsNoCompile(parent.getChildNodes()));
    }

    public ExprForgeConstantType getForgeConstantType() {
        return ExprForgeConstantType.NONCONST;
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return new InstrumentationBuilderExpr(this.getClass(), this, "ExprRelOpAnyOrAll", requiredType, codegenMethodScope, exprSymbol, codegenClassScope).qparam(constant(parent.getRelationalOpEnum().getExpressionText())).build();
    }

    public CodegenExpression evaluateCodegenUninstrumented(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return ExprRelationalOpAllAnyNodeForgeEval.codegen(this, codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public Class getEvaluationType() {
        return Boolean.class;
    }

    public ExprRelationalOpAllAnyNode getForgeRenderable() {
        return parent;
    }

    public RelationalOpEnum.Computer getComputer() {
        return computer;
    }

    public boolean isHasCollectionOrArray() {
        return hasCollectionOrArray;
    }
}
