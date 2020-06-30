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
package com.espertech.esper.common.internal.epl.expression.dot.core;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRelational;
import com.espertech.esper.common.internal.epl.enummethod.dot.ExprDotForgeProperty;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.rettype.EPChainableType;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ExprDotForgeEventArrayAtIndex implements ExprDotForge {
    private final EPChainableType returnType;
    private final ExprNode indexExpression;

    public ExprDotForgeEventArrayAtIndex(EPChainableType returnType, ExprNode indexExpression) {
        this.returnType = returnType;
        this.indexExpression = indexExpression;
    }

    public EPChainableType getTypeInfo() {
        return returnType;
    }

    public void visit(ExprDotEvalVisitor visitor) {
        visitor.visitArraySingleItemSource();
    }

    public ExprDotEval getDotEvaluator() {
        return new ExprDotEval() {
            public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
                EventBean[] events = (EventBean[]) target;
                if (events == null) {
                    return null;
                }
                Integer index = (Integer) indexExpression.getForge().getExprEvaluator().evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
                if (index == null) {
                    return null;
                }
                return events[index];
            }

            public ExprDotForge getDotForge() {
                return ExprDotForgeEventArrayAtIndex.this;
            }
        };
    }

    public CodegenExpression codegen(CodegenExpression inner, EPTypeClass innerType, CodegenMethodScope parent, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(EventBean.EPTYPE, ExprDotForgeProperty.class, classScope).addParam(EventBean.EPTYPEARRAY, "target").addParam(EPTypePremade.INTEGERBOXED.getEPType(), "index");
        method.getBlock()
            .ifNullReturnNull(ref("target"))
            .ifCondition(relational(ref("index"), CodegenExpressionRelational.CodegenRelational.GE, arrayLength(ref("target"))))
            .blockThrow(newInstance(EPException.EPTYPE, concat(constant("Array length "), arrayLength(ref("target")), constant(" less than index "), ref("index"))))
            .methodReturn(arrayAtIndex(ref("target"), cast(EPTypePremade.INTEGERPRIMITIVE.getEPType(), ref("index"))));
        return localMethod(method, inner, indexExpression.getForge().evaluateCodegen(EPTypePremade.INTEGERBOXED.getEPType(), method, symbols, classScope));
    }
}
