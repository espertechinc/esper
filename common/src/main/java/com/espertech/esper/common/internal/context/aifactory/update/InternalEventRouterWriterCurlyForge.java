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
package com.espertech.esper.common.internal.context.aifactory.update;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen;
import com.espertech.esper.common.internal.epl.variable.core.VariableTriggerWriteArrayElementForge;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class InternalEventRouterWriterCurlyForge extends InternalEventRouterWriterForge {

    private final ExprNode expression;

    public InternalEventRouterWriterCurlyForge(ExprNode expression) {
        this.expression = expression;
    }

    public CodegenExpression codegen(InternalEventRouterWriterForge writer, CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(InternalEventRouterWriterCurly.EPTYPE, this.getClass(), classScope);
        method.getBlock()
            .declareVarNewInstance(InternalEventRouterWriterCurly.EPTYPE, "desc")
            .exprDotMethod(ref("desc"), "setExpression",
                ExprNodeUtilityCodegen.codegenEvaluator(expression.getForge(), method, VariableTriggerWriteArrayElementForge.class, classScope))
            .methodReturn(ref("desc"));
        return localMethod(method);
    }
}
