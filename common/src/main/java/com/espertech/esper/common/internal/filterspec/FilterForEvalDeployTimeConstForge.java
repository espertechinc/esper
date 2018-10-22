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
import com.espertech.esper.common.internal.util.SimpleNumberCoercer;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.localMethod;
import static com.espertech.esper.common.internal.filterspec.FilterSpecParam.GET_FILTER_VALUE_FP;
import static com.espertech.esper.common.internal.filterspec.FilterSpecParam.GET_FILTER_VALUE_REFS;

/**
 * Event property value in a list of values following an in-keyword.
 */
public class FilterForEvalDeployTimeConstForge implements FilterSpecParamInValueForge {
    private transient final ExprNodeDeployTimeConst deployTimeConst;
    private transient final SimpleNumberCoercer numberCoercer;
    private transient final Class returnType;

    public FilterForEvalDeployTimeConstForge(ExprNodeDeployTimeConst deployTimeConst, SimpleNumberCoercer numberCoercer, Class returnType) {
        this.deployTimeConst = deployTimeConst;
        this.numberCoercer = numberCoercer;
        this.returnType = returnType;
    }

    public CodegenExpression makeCodegen(CodegenClassScope classScope, CodegenMethodScope parent) {
        CodegenMethod method = parent.makeChild(Object.class, this.getClass(), classScope).addParam(GET_FILTER_VALUE_FP);

        CodegenExpression value = deployTimeConst.codegenGetDeployTimeConstValue(classScope);
        if (numberCoercer != null) {
            value = numberCoercer.coerceCodegenMayNullBoxed(value, returnType, method, classScope);
        }
        method.getBlock().methodReturn(value);

        return localMethod(method, GET_FILTER_VALUE_REFS);
    }

    public Class getReturnType() {
        return returnType;
    }

    public boolean isConstant() {
        return false;
    }

    public Object getFilterValue(MatchedEventMap matchedEvents, ExprEvaluatorContext evaluatorContext) {
        throw ExprNodeUtilityMake.makeUnsupportedCompileTime();
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FilterForEvalDeployTimeConstForge that = (FilterForEvalDeployTimeConstForge) o;

        return deployTimeConst.equals(that.deployTimeConst);
    }

    public int hashCode() {
        return deployTimeConst.hashCode();
    }
}
