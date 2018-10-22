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
 * A String-typed value as a filter parameter representing a range.
 */
public class FilterForEvalConstantStringForge implements FilterSpecParamFilterForEvalForge {

    private final String theStringValue;

    /**
     * Ctor.
     *
     * @param theStringValue is the value of the range endpoint
     */
    public FilterForEvalConstantStringForge(String theStringValue) {
        this.theStringValue = theStringValue;
    }

    public final String getFilterValue(MatchedEventMap matchedEvents, ExprEvaluatorContext exprEvaluatorContext) {
        return theStringValue;
    }

    public final String toString() {
        return theStringValue;
    }

    public CodegenExpression makeCodegen(CodegenClassScope classScope, CodegenMethodScope parent) {
        return constant(theStringValue);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FilterForEvalConstantStringForge that = (FilterForEvalConstantStringForge) o;

        if (theStringValue != null ? !theStringValue.equals(that.theStringValue) : that.theStringValue != null)
            return false;

        return true;
    }

    public int hashCode() {
        return theStringValue != null ? theStringValue.hashCode() : 0;
    }
}
