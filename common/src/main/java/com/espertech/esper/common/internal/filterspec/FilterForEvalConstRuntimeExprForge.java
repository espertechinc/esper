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
package com.espertech.esper.common.internal.filterspec;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoMethodExpression;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCompare;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityMake;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * A Double-typed value as a filter parameter representing a range.
 */
public class FilterForEvalConstRuntimeExprForge implements FilterSpecParamFilterForEvalDoubleForge {
    private final ExprNode runtimeConstant;

    public FilterForEvalConstRuntimeExprForge(ExprNode runtimeConstant) {
        this.runtimeConstant = runtimeConstant;
    }

    public CodegenExpression makeCodegen(CodegenClassScope classScope, CodegenMethodScope parent) {
        CodegenMethod method = parent.makeChild(double.class, this.getClass(), classScope);
        CodegenMethod result = CodegenLegoMethodExpression.codegenExpression(runtimeConstant.getForge(), method, classScope);
        method.getBlock().methodReturn(localMethod(result, constantNull(), constantTrue(), constantNull()));
        return localMethod(method);
    }

    public Object getFilterValue(MatchedEventMap matchedEvents, ExprEvaluatorContext evaluatorContext) {
        throw ExprNodeUtilityMake.makeUnsupportedCompileTime();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof FilterForEvalConstRuntimeExprForge)) {
            return false;
        }

        FilterForEvalConstRuntimeExprForge other = (FilterForEvalConstRuntimeExprForge) obj;
        return ExprNodeUtilityCompare.deepEquals(other.runtimeConstant, runtimeConstant, true);
    }

    public int hashCode() {
        return 0;
    }
}
