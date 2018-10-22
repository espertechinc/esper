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
import com.espertech.esper.common.internal.util.JavaClassHelper;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.filterspec.FilterSpecParam.GET_FILTER_VALUE_FP;
import static com.espertech.esper.common.internal.filterspec.FilterSpecParam.GET_FILTER_VALUE_REFS;

/**
 * Event property value in a list of values following an in-keyword.
 */
public class FilterForEvalEventPropIndexedForge implements FilterSpecParamInValueForge {
    private final String resultEventAsName;
    private final int resultEventIndex;
    private final String resultEventProperty;
    private final boolean isMustCoerce;
    private final Class coercionType;
    private final EventType eventType;

    public FilterForEvalEventPropIndexedForge(String resultEventAsName, int resultEventindex, String resultEventProperty, EventType eventType, boolean isMustCoerce, Class coercionType) {
        this.resultEventAsName = resultEventAsName;
        this.resultEventProperty = resultEventProperty;
        this.resultEventIndex = resultEventindex;
        this.coercionType = coercionType;
        this.isMustCoerce = isMustCoerce;
        this.eventType = eventType;
    }

    public Class getReturnType() {
        return coercionType;
    }

    public boolean constant() {
        return false;
    }

    public CodegenExpression makeCodegen(CodegenClassScope classScope, CodegenMethodScope parent) {
        EventPropertyGetterSPI getterSPI = ((EventTypeSPI) eventType).getGetterSPI(resultEventProperty);
        CodegenMethod method = parent.makeChild(Object.class, this.getClass(), classScope).addParam(GET_FILTER_VALUE_FP);
        method.getBlock()
                .declareVar(EventBean[].class, "events", cast(EventBean[].class, exprDotMethod(ref("matchedEvents"), "getMatchingEventAsObjectByTag", CodegenExpressionBuilder.constant(resultEventAsName))))
                .declareVar(Object.class, "value", constantNull())
                .ifRefNotNull("events")
                .assignRef("value", getterSPI.eventBeanGetCodegen(arrayAtIndex(ref("events"), CodegenExpressionBuilder.constant(resultEventIndex)), method, classScope))
                .blockEnd();

        if (isMustCoerce) {
            method.getBlock().assignRef("value", JavaClassHelper.coerceNumberToBoxedCodegen(ref("value"), Object.class, coercionType));
        }
        method.getBlock().methodReturn(ref("value"));
        return localMethod(method, GET_FILTER_VALUE_REFS);
    }

    public boolean isConstant() {
        return false;
    }

    public Object getFilterValue(MatchedEventMap matchedEvents, ExprEvaluatorContext exprEvaluatorContext) {
        throw ExprNodeUtilityMake.makeUnsupportedCompileTime();
    }

    /**
     * Returns the tag used for the event property.
     *
     * @return tag
     */
    public String getResultEventAsName() {
        return resultEventAsName;
    }

    /**
     * Returns the event property name.
     *
     * @return property name
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

        if (!(obj instanceof FilterForEvalEventPropIndexedForge)) {
            return false;
        }

        FilterForEvalEventPropIndexedForge other = (FilterForEvalEventPropIndexedForge) obj;
        if ((other.resultEventAsName.equals(this.resultEventAsName)) &&
                (other.resultEventProperty.equals(this.resultEventProperty))) {
            return true;
        }

        return false;
    }

    public int hashCode() {
        return resultEventProperty.hashCode();
    }
}
