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
package com.espertech.esper.common.internal.epl.variable.core;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen;
import com.espertech.esper.common.internal.util.TypeWidenerFactory;
import com.espertech.esper.common.internal.util.TypeWidenerSPI;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class VariableTriggerWriteArrayElementForge extends VariableTriggerWriteForge {
    private final String variableName;
    private final ExprForge indexExpression;
    private final TypeWidenerSPI widener;

    public VariableTriggerWriteArrayElementForge(String variableName, ExprForge indexExpression, TypeWidenerSPI widener) {
        this.variableName = variableName;
        this.indexExpression = indexExpression;
        this.widener = widener;
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(VariableTriggerWriteArrayElement.class, this.getClass(), classScope);
        method.getBlock()
            .declareVar(VariableTriggerWriteArrayElement.class, "desc", newInstance(VariableTriggerWriteArrayElement.class))
            .exprDotMethod(ref("desc"), "setVariableName", constant(variableName))
            .exprDotMethod(ref("desc"), "setIndexExpression",
                ExprNodeUtilityCodegen.codegenEvaluator(indexExpression, method, VariableTriggerWriteArrayElementForge.class, classScope))
            .exprDotMethod(ref("desc"), "setTypeWidener", widener == null ? constantNull() : TypeWidenerFactory.codegenWidener(widener, method, this.getClass(), classScope))
            .methodReturn(ref("desc"));
        return localMethod(method);
    }
}
