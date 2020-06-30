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
package com.espertech.esper.common.internal.epl.expression.filter;

import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoCast;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.*;

import java.io.StringWriter;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.cast;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.exprDotMethod;

public class ExprFilterReboolValueNode extends ExprNodeBase implements ExprForge {
    private final EPTypeClass returnType;

    public ExprFilterReboolValueNode(EPTypeClass returnType) {
        this.returnType = returnType;
    }

    public void toPrecedenceFreeEPL(StringWriter writer, ExprNodeRenderableFlags flags) {
        writer.append("?");
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.UNARY;
    }

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
        throw new UnsupportedOperationException("Compare is not available");
    }

    public ExprForge getForge() {
        return this;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        // nothing to validate
        return null;
    }

    public ExprEvaluator getExprEvaluator() {
        throw new UnsupportedOperationException("Evaluator not available");
    }

    public CodegenExpression evaluateCodegen(EPTypeClass requiredType, CodegenMethodScope parent, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope) {
        return cast(requiredType, CodegenLegoCast.castSafeFromObjectType(requiredType, exprDotMethod(symbols.getAddExprEvalCtx(parent), "getFilterReboolConstant")));
    }

    public EPTypeClass getEvaluationType() {
        return returnType;
    }

    public ExprForgeConstantType getForgeConstantType() {
        return ExprForgeConstantType.NONCONST;
    }

    public ExprNodeRenderable getForgeRenderable() {
        return new ExprNodeRenderable() {
            public void toEPL(StringWriter writer, ExprPrecedenceEnum parentPrecedence, ExprNodeRenderableFlags flags) {
                writer.append(ExprFilterReboolValueNode.class.getSimpleName()).append("(").append(returnType.getClass().getSimpleName()).append(")");
            }
        };
    }
}
