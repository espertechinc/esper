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
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationBuilderExpr;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.io.StringWriter;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Represents a NOT expression in an expression tree.
 */
public class ExprNotNode extends ExprNodeBase implements ExprEvaluator, ExprForgeInstrumentable {
    private transient ExprEvaluator evaluator;

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        // Must have a single child node
        if (this.getChildNodes().length != 1) {
            throw new ExprValidationException("The NOT node requires exactly 1 child node");
        }

        ExprForge forge = this.getChildNodes()[0].getForge();
        Class childType = forge.getEvaluationType();
        if (!JavaClassHelper.isBoolean(childType)) {
            throw new ExprValidationException("Incorrect use of NOT clause, sub-expressions do not return boolean");
        }
        evaluator = forge.getExprEvaluator();
        return null;
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public ExprForge getForge() {
        return this;
    }

    public ExprNode getForgeRenderable() {
        return this;
    }

    public CodegenExpression evaluateCodegenUninstrumented(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        ExprForge child = this.getChildNodes()[0].getForge();
        if (child.getEvaluationType() == boolean.class) {
            not(child.evaluateCodegen(requiredType, codegenMethodScope, exprSymbol, codegenClassScope));
        }
        CodegenMethod methodNode = codegenMethodScope.makeChild(Boolean.class, ExprNotNode.class, codegenClassScope);
        methodNode.getBlock()
                .declareVar(Boolean.class, "b", child.evaluateCodegen(Boolean.class, methodNode, exprSymbol, codegenClassScope))
                .ifRefNullReturnNull("b")
                .methodReturn(not(ref("b")));
        return localMethod(methodNode);
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return new InstrumentationBuilderExpr(this.getClass(), this, "ExprNot", requiredType, codegenMethodScope, exprSymbol, codegenClassScope).build();
    }

    public Class getEvaluationType() {
        return Boolean.class;
    }

    public boolean isConstantResult() {
        return false;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        Boolean evaluated = (Boolean) evaluator.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        if (evaluated == null) {
            return null;
        }
        return !evaluated;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.append("not ");
        this.getChildNodes()[0].toEPL(writer, getPrecedence());
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.NEGATED;
    }

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
        if (!(node instanceof ExprNotNode)) {
            return false;
        }
        return true;
    }

    public ExprForgeConstantType getForgeConstantType() {
        return ExprForgeConstantType.NONCONST;
    }
}
