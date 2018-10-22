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
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeDeployTimeConst;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityMake;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.cast;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.localMethod;
import static com.espertech.esper.common.internal.filterspec.FilterSpecParam.GET_FILTER_VALUE_FP;
import static com.espertech.esper.common.internal.filterspec.FilterSpecParam.GET_FILTER_VALUE_REFS;

public class FilterForEvalDeployTimeConstStringForge implements FilterSpecParamFilterForEvalDoubleForge {

    private final ExprNodeDeployTimeConst deployTimeConst;

    public FilterForEvalDeployTimeConstStringForge(ExprNodeDeployTimeConst deployTimeConst) {
        this.deployTimeConst = deployTimeConst;
    }

    public CodegenExpression makeCodegen(CodegenClassScope classScope, CodegenMethodScope parent) {
        CodegenMethod method = parent.makeChild(Object.class, this.getClass(), classScope).addParam(GET_FILTER_VALUE_FP);
        method.getBlock()
                .methodReturn(cast(String.class, deployTimeConst.codegenGetDeployTimeConstValue(classScope)));
        return localMethod(method, GET_FILTER_VALUE_REFS);
    }

    public Double getFilterValue(MatchedEventMap matchedEvents, ExprEvaluatorContext exprEvaluatorContext) {
        throw ExprNodeUtilityMake.makeUnsupportedCompileTime();
    }

    public Double getFilterValueDouble(MatchedEventMap matchedEvents, ExprEvaluatorContext exprEvaluatorContext) {
        return getFilterValue(matchedEvents, exprEvaluatorContext);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FilterForEvalDeployTimeConstStringForge that = (FilterForEvalDeployTimeConstStringForge) o;

        return deployTimeConst.equals(that.deployTimeConst);
    }

    public int hashCode() {
        return deployTimeConst.hashCode();
    }
}
