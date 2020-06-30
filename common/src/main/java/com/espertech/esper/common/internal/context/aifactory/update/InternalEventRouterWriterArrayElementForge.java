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
import com.espertech.esper.common.internal.util.TypeWidenerFactory;
import com.espertech.esper.common.internal.util.TypeWidenerSPI;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class InternalEventRouterWriterArrayElementForge extends InternalEventRouterWriterForge {

    private final ExprNode indexExpression;
    private final ExprNode rhsExpression;
    private final TypeWidenerSPI widener;
    private final String propertyName;

    public InternalEventRouterWriterArrayElementForge(ExprNode indexExpression, ExprNode rhsExpression, TypeWidenerSPI widener, String propertyName) {
        this.indexExpression = indexExpression;
        this.rhsExpression = rhsExpression;
        this.widener = widener;
        this.propertyName = propertyName;
    }

    public CodegenExpression codegen(InternalEventRouterWriterForge writer, CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(InternalEventRouterWriterArrayElement.EPTYPE, this.getClass(), classScope);
        method.getBlock()
            .declareVarNewInstance(InternalEventRouterWriterArrayElement.EPTYPE, "desc")
            .exprDotMethod(ref("desc"), "setIndexExpression",
                ExprNodeUtilityCodegen.codegenEvaluator(indexExpression.getForge(), method, VariableTriggerWriteArrayElementForge.class, classScope))
            .exprDotMethod(ref("desc"), "setRhsExpression",
                ExprNodeUtilityCodegen.codegenEvaluator(rhsExpression.getForge(), method, VariableTriggerWriteArrayElementForge.class, classScope))
            .exprDotMethod(ref("desc"), "setTypeWidener", widener == null ? constantNull() : TypeWidenerFactory.codegenWidener(widener, method, this.getClass(), classScope))
            .exprDotMethod(ref("desc"), "setPropertyName", constant(propertyName))
            .methodReturn(ref("desc"));
        return localMethod(method);
    }
}
