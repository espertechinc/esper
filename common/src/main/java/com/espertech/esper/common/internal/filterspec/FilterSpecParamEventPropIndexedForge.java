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
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionNewAnonymousClass;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbolWEventType;
import com.espertech.esper.common.internal.epl.expression.core.ExprFilterSpecLookupable;
import com.espertech.esper.common.internal.epl.expression.core.ExprFilterSpecLookupableForge;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterSPI;
import com.espertech.esper.common.internal.event.core.EventTypeSPI;
import com.espertech.esper.common.internal.util.SimpleNumberCoercer;

import java.util.Arrays;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * This class represents a filter parameter containing a reference to another event's property
 * in the event pattern result, for use to describe a filter parameter in a filter specification.
 */
public final class FilterSpecParamEventPropIndexedForge extends FilterSpecParamForge {
    private final String resultEventAsName;
    private final int resultEventIndex;
    private final String resultEventProperty;
    private final EventType eventType;
    private final boolean isMustCoerce;
    private final transient SimpleNumberCoercer numberCoercer;
    private final EPTypeClass coercionType;

    /**
     * Constructor.
     *
     * @param lookupable          is the lookupable
     * @param filterOperator      is the type of compare
     * @param resultEventAsName   is the name of the result event from which to get a property value to compare
     * @param resultEventIndex    index
     * @param resultEventProperty is the name of the property to get from the named result event
     * @param eventType           event type
     * @param isMustCoerce        indicates on whether numeric coercion must be performed
     * @param numberCoercer       interface to use to perform coercion
     * @param coercionType        indicates the numeric coercion type to use
     * @throws IllegalArgumentException if an operator was supplied that does not take a single constant value
     */
    public FilterSpecParamEventPropIndexedForge(ExprFilterSpecLookupableForge lookupable, FilterOperator filterOperator, String resultEventAsName,
                                                int resultEventIndex, String resultEventProperty, EventType eventType, boolean isMustCoerce,
                                                SimpleNumberCoercer numberCoercer, EPTypeClass coercionType)
            throws IllegalArgumentException {
        super(lookupable, filterOperator);
        this.resultEventAsName = resultEventAsName;
        this.resultEventIndex = resultEventIndex;
        this.resultEventProperty = resultEventProperty;
        this.eventType = eventType;
        this.isMustCoerce = isMustCoerce;
        this.numberCoercer = numberCoercer;
        this.coercionType = coercionType;

        if (filterOperator.isRangeOperator()) {
            throw new IllegalArgumentException("Illegal filter operator " + filterOperator + " supplied to " +
                    "event property filter parameter");
        }
    }

    public CodegenExpression makeCodegen(CodegenClassScope classScope, CodegenMethodScope parent, SAIFFInitializeSymbolWEventType symbols) {
        EventPropertyGetterSPI getterSPI = ((EventTypeSPI) eventType).getGetterSPI(resultEventProperty);
        CodegenMethod method = parent.makeChild(FilterSpecParam.EPTYPE, FilterSpecParamConstantForge.class, classScope);

        method.getBlock()
                .declareVar(ExprFilterSpecLookupable.EPTYPE, "lookupable", localMethod(lookupable.makeCodegen(method, symbols, classScope)))
                .declareVar(ExprFilterSpecLookupable.EPTYPE_FILTEROPERATOR, "op", enumValue(FilterOperator.class, filterOperator.name()));

        CodegenExpressionNewAnonymousClass param = newAnonymousClass(method.getBlock(), FilterSpecParam.EPTYPE, Arrays.asList(ref("lookupable"), ref("op")));
        CodegenMethod getFilterValue = CodegenMethod.makeParentNode(FilterValueSetParam.EPTYPE, this.getClass(), classScope).addParam(FilterSpecParam.GET_FILTER_VALUE_FP);
        param.addMethod("getFilterValue", getFilterValue);
        getFilterValue.getBlock()
                .declareVar(EventBean.EPTYPEARRAY, "events", cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("matchedEvents"), "getMatchingEventAsObjectByTag", constant(resultEventAsName))))
                .declareVar(EPTypePremade.OBJECT.getEPType(), "value", constantNull())
                .ifRefNotNull("events")
                .assignRef("value", getterSPI.eventBeanGetCodegen(arrayAtIndex(ref("events"), constant(resultEventIndex)), method, classScope))
                .blockEnd();

        if (isMustCoerce) {
            getFilterValue.getBlock().assignRef("value", numberCoercer.coerceCodegenMayNullBoxed(cast(EPTypePremade.NUMBER.getEPType(), ref("value")), EPTypePremade.NUMBER.getEPType(), method, classScope));
        }
        getFilterValue.getBlock().methodReturn(FilterValueSetParamImpl.codegenNew(ref("value")));

        method.getBlock().methodReturn(param);
        return localMethod(method);
    }

    /**
     * Returns true if numeric coercion is required, or false if not
     *
     * @return true to coerce at runtime
     */
    public boolean isMustCoerce() {
        return isMustCoerce;
    }

    /**
     * Returns the numeric coercion type.
     *
     * @return type to coerce to
     */
    public EPTypeClass getCoercionType() {
        return coercionType;
    }

    /**
     * Returns tag for result event.
     *
     * @return tag
     */
    public String getResultEventAsName() {
        return resultEventAsName;
    }

    /**
     * Returns the property of the result event.
     *
     * @return property name
     */
    public String getResultEventProperty() {
        return resultEventProperty;
    }

    public EventType getEventType() {
        return eventType;
    }

    /**
     * Returns the index.
     *
     * @return index
     */
    public int getResultEventIndex() {
        return resultEventIndex;
    }

    public final String toString() {
        return super.toString() +
                " resultEventAsName=" + resultEventAsName +
                " resultEventProperty=" + resultEventProperty;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof FilterSpecParamEventPropIndexedForge)) {
            return false;
        }

        FilterSpecParamEventPropIndexedForge other = (FilterSpecParamEventPropIndexedForge) obj;
        if (!super.equals(other)) {
            return false;
        }

        if ((!this.resultEventAsName.equals(other.resultEventAsName)) ||
                (!this.resultEventProperty.equals(other.resultEventProperty)) ||
                (this.resultEventIndex != other.resultEventIndex)) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + resultEventProperty.hashCode();
        return result;
    }

    public void valueExprToString(StringBuilder out, int i) {
        out.append("indexed event property '").append(resultEventProperty).append("'");
    }
}
