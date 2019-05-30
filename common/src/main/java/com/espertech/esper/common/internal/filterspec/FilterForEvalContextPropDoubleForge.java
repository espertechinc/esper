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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterSPI;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames.REF_EXPREVALCONTEXT;
import static com.espertech.esper.common.internal.filterspec.FilterSpecParam.GET_FILTER_VALUE_FP;
import static com.espertech.esper.common.internal.filterspec.FilterSpecParam.GET_FILTER_VALUE_REFS;

public class FilterForEvalContextPropDoubleForge implements FilterSpecParamFilterForEvalDoubleForge {

    private transient final EventPropertyGetterSPI getter;
    private final String propertyName;

    public FilterForEvalContextPropDoubleForge(EventPropertyGetterSPI getter, String propertyName) {
        this.getter = getter;
        this.propertyName = propertyName;
    }

    public CodegenExpression makeCodegen(CodegenClassScope classScope, CodegenMethodScope parent) {
        CodegenMethod method = parent.makeChild(Object.class, this.getClass(), classScope).addParam(GET_FILTER_VALUE_FP);

        method.getBlock()
                .declareVar(EventBean.class, "props", exprDotMethod(REF_EXPREVALCONTEXT, "getContextProperties"))
                .ifNullReturnNull(ref("props"))
                .declareVar(Object.class, "result", getter.eventBeanGetCodegen(ref("props"), method, classScope))
                .ifRefNullReturnNull("result")
                .methodReturn(exprDotMethod(cast(Number.class, ref("result")), "doubleValue"));

        return localMethod(method, GET_FILTER_VALUE_REFS);
    }

    public Double getFilterValue(MatchedEventMap matchedEvents, ExprEvaluatorContext exprEvaluatorContext) {
        if (exprEvaluatorContext.getContextProperties() == null) {
            return null;
        }
        Object object = getter.get(exprEvaluatorContext.getContextProperties());
        if (object == null) {
            return null;
        }
        Number value = (Number) object;
        return value.doubleValue();
    }

    public Double getFilterValueDouble(MatchedEventMap matchedEvents, ExprEvaluatorContext exprEvaluatorContext) {
        return getFilterValue(matchedEvents, exprEvaluatorContext);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FilterForEvalContextPropDoubleForge that = (FilterForEvalContextPropDoubleForge) o;

        return propertyName.equals(that.propertyName);
    }

    public int hashCode() {
        return propertyName.hashCode();
    }
}
