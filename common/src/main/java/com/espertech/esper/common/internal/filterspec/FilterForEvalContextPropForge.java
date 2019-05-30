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
import com.espertech.esper.common.internal.util.SimpleNumberCoercer;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames.REF_EXPREVALCONTEXT;
import static com.espertech.esper.common.internal.filterspec.FilterSpecParam.GET_FILTER_VALUE_FP;
import static com.espertech.esper.common.internal.filterspec.FilterSpecParam.GET_FILTER_VALUE_REFS;

/**
 * Event property value in a list of values following an in-keyword.
 */
public class FilterForEvalContextPropForge implements FilterSpecParamInValueForge {
    private final String propertyName;
    private transient final EventPropertyGetterSPI getter;
    private transient final SimpleNumberCoercer numberCoercer;
    private transient final Class returnType;

    public FilterForEvalContextPropForge(String propertyName, EventPropertyGetterSPI getter, SimpleNumberCoercer coercer, Class returnType) {
        this.propertyName = propertyName;
        this.getter = getter;
        this.numberCoercer = coercer;
        this.returnType = returnType;
    }

    public CodegenExpression makeCodegen(CodegenClassScope classScope, CodegenMethodScope parent) {
        CodegenMethod method = parent.makeChild(Object.class, this.getClass(), classScope).addParam(GET_FILTER_VALUE_FP);

        method.getBlock()
                .declareVar(EventBean.class, "props", exprDotMethod(REF_EXPREVALCONTEXT, "getContextProperties"))
                .ifNullReturnNull(ref("props"))
                .declareVar(Object.class, "result", getter.eventBeanGetCodegen(ref("props"), method, classScope));
        if (numberCoercer != null) {
            method.getBlock().assignRef("result", numberCoercer.coerceCodegenMayNullBoxed(cast(Number.class, ref("result")), Number.class, method, classScope));
        }
        method.getBlock().methodReturn(ref("result"));

        return localMethod(method, GET_FILTER_VALUE_REFS);
    }

    public Class getReturnType() {
        return returnType;
    }

    public boolean isConstant() {
        return false;
    }

    public Object getFilterValue(MatchedEventMap matchedEvents, ExprEvaluatorContext evaluatorContext) {
        if (evaluatorContext.getContextProperties() == null) {
            return null;
        }
        Object result = getter.get(evaluatorContext.getContextProperties());

        if (numberCoercer == null) {
            return result;
        }
        return numberCoercer.coerceBoxed((Number) result);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FilterForEvalContextPropForge that = (FilterForEvalContextPropForge) o;

        if (!propertyName.equals(that.propertyName)) return false;

        return true;
    }

    public int hashCode() {
        return propertyName.hashCode();
    }
}
