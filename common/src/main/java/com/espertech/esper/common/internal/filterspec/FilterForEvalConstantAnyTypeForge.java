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
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;

/**
 * Constant value in a list of values following an in-keyword.
 */
public class FilterForEvalConstantAnyTypeForge implements FilterSpecParamInValueForge {
    private Object constant;

    /**
     * Ctor.
     *
     * @param constant is the constant value
     */
    public FilterForEvalConstantAnyTypeForge(Object constant) {
        this.constant = constant;
    }

    public Class getReturnType() {
        return constant == null ? null : constant.getClass();
    }

    public boolean isConstant() {
        return true;
    }

    /**
     * Returns the constant value.
     *
     * @return constant
     */
    public Object getConstant() {
        return constant;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        FilterForEvalConstantAnyTypeForge that = (FilterForEvalConstantAnyTypeForge) o;

        if (constant != null ? !constant.equals(that.constant) : that.constant != null) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        return constant != null ? constant.hashCode() : 0;
    }

    public Object getFilterValue(MatchedEventMap matchedEvents, ExprEvaluatorContext evaluatorContext) {
        return constant;
    }

    public CodegenExpression makeCodegen(CodegenClassScope classScope, CodegenMethodScope parent) {
        return constant(constant);
    }
}
