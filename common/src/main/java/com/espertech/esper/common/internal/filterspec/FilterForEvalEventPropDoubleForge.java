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

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.filterspec.FilterSpecParam.GET_FILTER_VALUE_FP;
import static com.espertech.esper.common.internal.filterspec.FilterSpecParam.GET_FILTER_VALUE_REFS;

/**
 * An event property as a filter parameter representing a range.
 */
public class FilterForEvalEventPropDoubleForge implements FilterSpecParamFilterForEvalDoubleForge {
    private final String resultEventAsName;
    private final String resultEventProperty;
    private final ExprIdentNodeEvaluator exprIdentNodeEvaluator;

    public FilterForEvalEventPropDoubleForge(String resultEventAsName, String resultEventProperty, ExprIdentNodeEvaluator exprIdentNodeEvaluator) {
        this.resultEventAsName = resultEventAsName;
        this.resultEventProperty = resultEventProperty;
        this.exprIdentNodeEvaluator = exprIdentNodeEvaluator;
    }

    /**
     * Returns the tag name or stream name to use for the event property.
     *
     * @return tag name
     */
    public String getResultEventAsName() {
        return resultEventAsName;
    }

    /**
     * Returns the name of the event property.
     *
     * @return event property name
     */
    public String getResultEventProperty() {
        return resultEventProperty;
    }

    public final String toString() {
        return "resultEventProp=" + resultEventAsName + '.' + resultEventProperty;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof FilterForEvalEventPropDoubleForge)) {
            return false;
        }

        FilterForEvalEventPropDoubleForge other = (FilterForEvalEventPropDoubleForge) obj;
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
                .declareVar(Number.class, "value", cast(Number.class, get))
                .ifRefNull("value").blockReturn(constantNull())
                .methodReturn(exprDotMethod(ref("value"), "doubleValue"));

        return localMethod(method, GET_FILTER_VALUE_REFS);
    }

    public Object getFilterValue(MatchedEventMap matchedEvents, ExprEvaluatorContext exprEvaluatorContext) {
        throw new IllegalStateException("Cannot evaluate");
    }
}
