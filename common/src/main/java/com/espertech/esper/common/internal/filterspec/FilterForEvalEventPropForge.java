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
import com.espertech.esper.common.internal.epl.expression.core.ExprIdentNodeEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityMake;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.filterspec.FilterSpecParam.GET_FILTER_VALUE_FP;
import static com.espertech.esper.common.internal.filterspec.FilterSpecParam.GET_FILTER_VALUE_REFS;

/**
 * Event property value in a list of values following an in-keyword.
 */
public class FilterForEvalEventPropForge implements FilterSpecParamInValueForge {
    private final String resultEventAsName;
    private final String resultEventProperty;
    private final ExprIdentNodeEvaluator exprIdentNodeEvaluator;
    private final boolean isMustCoerce;
    private final Class coercionType;

    public FilterForEvalEventPropForge(String resultEventAsName, String resultEventProperty, ExprIdentNodeEvaluator exprIdentNodeEvaluator, boolean isMustCoerce, Class coercionType) {
        this.resultEventAsName = resultEventAsName;
        this.resultEventProperty = resultEventProperty;
        this.exprIdentNodeEvaluator = exprIdentNodeEvaluator;
        this.coercionType = coercionType;
        this.isMustCoerce = isMustCoerce;
    }

    public Class getReturnType() {
        return coercionType;
    }

    public boolean isConstant() {
        return false;
    }

    public final Object getFilterValue(MatchedEventMap matchedEvents, ExprEvaluatorContext evaluatorContext) {
        throw ExprNodeUtilityMake.makeUnsupportedCompileTime();
    }

    public final String toString() {
        return "resultEventProp=" + resultEventAsName + '.' + resultEventProperty;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof FilterForEvalEventPropForge)) {
            return false;
        }

        FilterForEvalEventPropForge other = (FilterForEvalEventPropForge) obj;
        if ((other.resultEventAsName.equals(this.resultEventAsName)) &&
                (other.resultEventProperty.equals(this.resultEventProperty))) {
            return true;
        }

        return false;
    }

    public int hashCode() {
        return resultEventProperty.hashCode();
    }

    public CodegenExpression makeCodegen(CodegenClassScope classScope, CodegenMethodScope parent) {
        CodegenMethod method = parent.makeChild(Object.class, this.getClass(), classScope).addParam(GET_FILTER_VALUE_FP);
        CodegenExpression get = exprIdentNodeEvaluator.getGetter().eventBeanGetCodegen(ref("event"), method, classScope);

        method.getBlock()
                .declareVar(EventBean.class, "event", exprDotMethod(ref("matchedEvents"), "getMatchingEventByTag", constant(resultEventAsName)))
                .ifNull(ref("event")).blockThrow(newInstance(IllegalStateException.class, constant("Matching event named '" + resultEventAsName + "' not found in event result set")))
                .declareVar(Object.class, "value", get);

        if (isMustCoerce) {
            method.getBlock().assignRef("value", JavaClassHelper.coerceNumberBoxedToBoxedCodegen(cast(Number.class, ref("value")), Number.class, coercionType));
        }
        method.getBlock().methodReturn(ref("value"));
        return localMethod(method, GET_FILTER_VALUE_REFS);
    }
}
