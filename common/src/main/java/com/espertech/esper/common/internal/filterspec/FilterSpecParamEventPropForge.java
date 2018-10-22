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
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionNewAnonymousClass;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbolWEventType;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprFilterSpecLookupable;
import com.espertech.esper.common.internal.epl.expression.core.ExprFilterSpecLookupableForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprIdentNodeEvaluator;
import com.espertech.esper.common.internal.settings.ClasspathImportServiceRuntime;
import com.espertech.esper.common.internal.util.SimpleNumberCoercer;

import java.lang.annotation.Annotation;
import java.util.Arrays;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * This class represents a filter parameter containing a reference to another event's property
 * in the event pattern result, for use to describe a filter parameter in a {@link FilterSpecActivatable} filter specification.
 */
public final class FilterSpecParamEventPropForge extends FilterSpecParamForge {
    private final String resultEventAsName;
    private final String resultEventProperty;
    private final ExprIdentNodeEvaluator exprIdentNodeEvaluator;
    private final boolean isMustCoerce;
    private transient final SimpleNumberCoercer numberCoercer;
    private final Class coercionType;
    private final String statementName;

    /**
     * Constructor.
     *
     * @param lookupable             is the property or function to get a lookup value
     * @param filterOperator         is the type of compare
     * @param resultEventAsName      is the name of the result event from which to get a property value to compare
     * @param resultEventProperty    is the name of the property to get from the named result event
     * @param isMustCoerce           indicates on whether numeric coercion must be performed
     * @param coercionType           indicates the numeric coercion type to use
     * @param numberCoercer          interface to use to perform coercion
     * @param statementName          statement name
     * @param exprIdentNodeEvaluator evaluator
     * @throws IllegalArgumentException if an operator was supplied that does not take a single constant value
     */
    public FilterSpecParamEventPropForge(ExprFilterSpecLookupableForge lookupable, FilterOperator filterOperator, String resultEventAsName,
                                         String resultEventProperty, ExprIdentNodeEvaluator exprIdentNodeEvaluator, boolean isMustCoerce,
                                         SimpleNumberCoercer numberCoercer, Class coercionType,
                                         String statementName)
            throws IllegalArgumentException {
        super(lookupable, filterOperator);
        this.resultEventAsName = resultEventAsName;
        this.resultEventProperty = resultEventProperty;
        this.exprIdentNodeEvaluator = exprIdentNodeEvaluator;
        this.isMustCoerce = isMustCoerce;
        this.numberCoercer = numberCoercer;
        this.coercionType = coercionType;
        this.statementName = statementName;

        if (filterOperator.isRangeOperator()) {
            throw new IllegalArgumentException("Illegal filter operator " + filterOperator + " supplied to " +
                    "event property filter parameter");
        }
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
    public Class getCoercionType() {
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

    public Object getFilterValue(MatchedEventMap matchedEvents, ExprEvaluatorContext exprEvaluatorContext, ClasspathImportServiceRuntime classpathImportService, Annotation[] annotations) {
        throw new IllegalStateException("Not possible to evaluate");
    }

    public ExprIdentNodeEvaluator getExprIdentNodeEvaluator() {
        return exprIdentNodeEvaluator;
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

        if (!(obj instanceof FilterSpecParamEventPropForge)) {
            return false;
        }

        FilterSpecParamEventPropForge other = (FilterSpecParamEventPropForge) obj;
        if (!super.equals(other)) {
            return false;
        }

        if ((!this.resultEventAsName.equals(other.resultEventAsName)) ||
                (!this.resultEventProperty.equals(other.resultEventProperty))) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + resultEventProperty.hashCode();
        return result;
    }

    public CodegenMethod makeCodegen(CodegenClassScope classScope, CodegenMethodScope parent, SAIFFInitializeSymbolWEventType symbols) {

        CodegenMethod method = parent.makeChild(FilterSpecParam.class, this.getClass(), classScope);
        CodegenExpression get = exprIdentNodeEvaluator.getGetter().eventBeanGetCodegen(ref("event"), method, classScope);

        method.getBlock()
                .declareVar(ExprFilterSpecLookupable.class, "lookupable", localMethod(lookupable.makeCodegen(method, symbols, classScope)))
                .declareVar(FilterOperator.class, "op", enumValue(FilterOperator.class, filterOperator.name()));

        CodegenExpressionNewAnonymousClass param = newAnonymousClass(method.getBlock(), FilterSpecParam.class, Arrays.asList(ref("lookupable"), ref("op")));
        CodegenMethod getFilterValue = CodegenMethod.makeParentNode(Object.class, this.getClass(), classScope).addParam(FilterSpecParam.GET_FILTER_VALUE_FP);
        param.addMethod("getFilterValue", getFilterValue);
        getFilterValue.getBlock()
                .declareVar(EventBean.class, "event", exprDotMethod(ref("matchedEvents"), "getMatchingEventByTag", constant(resultEventAsName)))
                .declareVar(Object.class, "value", constantNull())
                .ifRefNotNull("event")
                .assignRef("value", get)
                .blockEnd();

        if (isMustCoerce) {
            getFilterValue.getBlock().assignRef("value", numberCoercer.coerceCodegenMayNullBoxed(cast(Number.class, ref("value")), Number.class, method, classScope));
        }
        getFilterValue.getBlock().methodReturn(ref("value"));

        method.getBlock().methodReturn(param);
        return method;
    }
}
