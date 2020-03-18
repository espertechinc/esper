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

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class VariableTriggerWriteCurlyForge extends VariableTriggerWriteForge {
    private final String variableName;
    private final ExprForge expression;

    public VariableTriggerWriteCurlyForge(String variableName, ExprForge expression) {
        this.variableName = variableName;
        this.expression = expression;
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(VariableTriggerWriteCurly.class, this.getClass(), classScope);
        method.getBlock()
            .declareVar(VariableTriggerWriteCurly.class, "desc", newInstance(VariableTriggerWriteCurly.class))
            .exprDotMethod(ref("desc"), "setVariableName", constant(variableName))
            .exprDotMethod(ref("desc"), "setExpression", ExprNodeUtilityCodegen.codegenEvaluator(expression, method, VariableTriggerWriteCurlyForge.class, classScope))
            .methodReturn(ref("desc"));
        return localMethod(method);
    }
}
