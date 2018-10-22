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
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityMake;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterSPI;
import com.espertech.esper.common.internal.event.core.EventTypeSPI;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.filterspec.FilterSpecParam.GET_FILTER_VALUE_FP;
import static com.espertech.esper.common.internal.filterspec.FilterSpecParam.GET_FILTER_VALUE_REFS;

/**
 * An event property as a filter parameter representing a range.
 */
public class FilterForEvalEventPropIndexedDoubleForge implements FilterSpecParamFilterForEvalDoubleForge {
    private final String resultEventAsName;
    private final int resultEventIndex;
    private final String resultEventProperty;
    private final EventType eventType;

    public FilterForEvalEventPropIndexedDoubleForge(String resultEventAsName, int resultEventIndex, String resultEventProperty, EventType eventType) {
        this.resultEventAsName = resultEventAsName;
        this.resultEventIndex = resultEventIndex;
        this.resultEventProperty = resultEventProperty;
        this.eventType = eventType;
    }

    public CodegenExpression makeCodegen(CodegenClassScope classScope, CodegenMethodScope parent) {
        EventPropertyGetterSPI getterSPI = ((EventTypeSPI) eventType).getGetterSPI(resultEventProperty);
        CodegenMethod method = parent.makeChild(Object.class, this.getClass(), classScope).addParam(GET_FILTER_VALUE_FP);
        method.getBlock()
                .declareVar(EventBean[].class, "events", cast(EventBean[].class, exprDotMethod(ref("matchedEvents"), "getMatchingEventAsObjectByTag", CodegenExpressionBuilder.constant(resultEventAsName))))
                .declareVar(Number.class, "value", constantNull())
                .ifRefNotNull("events")
                .assignRef("value", cast(Number.class, getterSPI.eventBeanGetCodegen(arrayAtIndex(ref("events"), CodegenExpressionBuilder.constant(resultEventIndex)), method, classScope)))
                .blockEnd()
                .ifRefNullReturnNull("value")
                .methodReturn(exprDotMethod(ref("value"), "doubleValue"));
        return localMethod(method, GET_FILTER_VALUE_REFS);
    }

    public Double getFilterValue(MatchedEventMap matchedEvents, ExprEvaluatorContext exprEvaluatorContext) {
        throw ExprNodeUtilityMake.makeUnsupportedCompileTime();
    }

    public final String toString() {
        return "resultEventProp=" + resultEventAsName + '.' + resultEventProperty;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof FilterForEvalEventPropIndexedDoubleForge)) {
            return false;
        }

        FilterForEvalEventPropIndexedDoubleForge other = (FilterForEvalEventPropIndexedDoubleForge) obj;
        if ((other.resultEventAsName.equals(this.resultEventAsName)) &&
                (other.resultEventProperty.equals(this.resultEventProperty) &&
                        (other.resultEventIndex == resultEventIndex))) {
            return true;
        }

        return false;
    }

    public int hashCode() {
        return resultEventProperty.hashCode();
    }
}
