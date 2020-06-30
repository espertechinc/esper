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
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationBuilderExpr;

import java.io.StringWriter;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.util.JavaClassHelper.isTypeBoolean;

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
        if (!isTypeBoolean(forge.getEvaluationType())) {
            throw new ExprValidationException("Incorrect use of NOT clause, sub-expressions do not return boolean");
        }
        return null;
    }

    public ExprEvaluator getExprEvaluator() {
        initEvaluator();
        return this;
    }

    public ExprForge getForge() {
        return this;
    }

    public ExprNode getForgeRenderable() {
        return this;
    }

    public CodegenExpression evaluateCodegenUninstrumented(EPTypeClass requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        ExprForge child = this.getChildNodes()[0].getForge();
        EPTypeClass childType = (EPTypeClass) child.getEvaluationType();
        if (childType.getType() == boolean.class) {
            not(child.evaluateCodegen(requiredType, codegenMethodScope, exprSymbol, codegenClassScope));
        }
        CodegenMethod methodNode = codegenMethodScope.makeChild(EPTypePremade.BOOLEANBOXED.getEPType(), ExprNotNode.class, codegenClassScope);
        methodNode.getBlock()
                .declareVar(EPTypePremade.BOOLEANBOXED.getEPType(), "b", child.evaluateCodegen(EPTypePremade.BOOLEANPRIMITIVE.getEPType(), methodNode, exprSymbol, codegenClassScope))
                .ifRefNullReturnNull("b")
                .methodReturn(not(ref("b")));
        return localMethod(methodNode);
    }

    public CodegenExpression evaluateCodegen(EPTypeClass requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return new InstrumentationBuilderExpr(this.getClass(), this, "ExprNot", requiredType, codegenMethodScope, exprSymbol, codegenClassScope).build();
    }

    public EPTypeClass getEvaluationType() {
        return EPTypePremade.BOOLEANBOXED.getEPType();
    }

    public boolean isConstantResult() {
        return false;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        initEvaluator();
        Boolean evaluated = (Boolean) evaluator.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        if (evaluated == null) {
            return null;
        }
        return !evaluated;
    }

    public void toPrecedenceFreeEPL(StringWriter writer, ExprNodeRenderableFlags flags) {
        writer.append("not ");
        this.getChildNodes()[0].toEPL(writer, getPrecedence(), flags);
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

    private void initEvaluator() {
        if (evaluator == null) {
            evaluator = getChildNodes()[0].getForge().getExprEvaluator();
        }
    }
}
