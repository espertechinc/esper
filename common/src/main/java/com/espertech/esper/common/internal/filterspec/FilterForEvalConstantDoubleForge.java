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
 * A Double-typed value as a filter parameter representing a range.
 */
public class FilterForEvalConstantDoubleForge implements FilterSpecParamFilterForEvalDoubleForge {
    private final double doubleValue;

    public FilterForEvalConstantDoubleForge(double doubleValue) {
        this.doubleValue = doubleValue;
    }

    public CodegenExpression makeCodegen(CodegenClassScope classScope, CodegenMethodScope parent) {
        return constant(doubleValue);
    }

    public Object getFilterValue(MatchedEventMap matchedEvents, ExprEvaluatorContext evaluatorContext) {
        return doubleValue;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof FilterForEvalConstantDoubleForge)) {
            return false;
        }

        FilterForEvalConstantDoubleForge other = (FilterForEvalConstantDoubleForge) obj;
        return other.doubleValue == this.doubleValue;
    }

    public int hashCode() {
        long temp = doubleValue != +0.0d ? Double.doubleToLongBits(doubleValue) : 0L;
        return (int) (temp ^ (temp >>> 32));
    }
}
