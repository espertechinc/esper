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

import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationBuilderExpr;

import java.io.StringWriter;

import static com.espertech.esper.common.internal.util.JavaClassHelper.isTypeBoolean;

/**
 * Represents an OR expression in a filter expression tree.
 */
public class ExprOrNode extends ExprNodeBase implements ExprForgeInstrumentable {

    public EPTypeClass getEvaluationType() {
        return EPTypePremade.BOOLEANBOXED.getEPType();
    }

    public ExprForge getForge() {
        return this;
    }

    public ExprNode getForgeRenderable() {
        return this;
    }

    public ExprEvaluator getExprEvaluator() {
        return new ExprOrNodeEval(this, ExprNodeUtilityQuery.getEvaluatorsNoCompile(this.getChildNodes()));
    }

    public CodegenExpression evaluateCodegenUninstrumented(EPTypeClass requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return ExprOrNodeEval.codegen(this, codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public CodegenExpression evaluateCodegen(EPTypeClass requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return new InstrumentationBuilderExpr(this.getClass(), this, "ExprOr", requiredType, codegenMethodScope, exprSymbol, codegenClassScope).build();
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        // Sub-nodes must be returning boolean
        for (ExprNode child : getChildNodes()) {
            if (!isTypeBoolean(child.getForge().getEvaluationType())) {
                throw new ExprValidationException("Incorrect use of OR clause, sub-expressions do not return boolean");
            }
        }

        if (this.getChildNodes().length <= 1) {
            throw new ExprValidationException("The OR operator requires at least 2 child expressions");
        }
        return null;
    }

    public boolean isConstantResult() {
        return false;
    }

    public void toPrecedenceFreeEPL(StringWriter writer, ExprNodeRenderableFlags flags) {
        String appendStr = "";
        for (ExprNode child : this.getChildNodes()) {
            writer.append(appendStr);
            child.toEPL(writer, getPrecedence(), flags);
            appendStr = " or ";
        }
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.OR;
    }

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
        if (!(node instanceof ExprOrNode)) {
            return false;
        }

        return true;
    }

    public ExprForgeConstantType getForgeConstantType() {
        return ExprForgeConstantType.NONCONST;
    }
}
