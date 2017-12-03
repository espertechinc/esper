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
package com.espertech.esper.filter;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.filterspec.FilterSpecParamInValue;
import com.espertech.esper.filterspec.MatchedEventMap;
import com.espertech.esper.util.JavaClassHelper;

/**
 * Event property value in a list of values following an in-keyword.
 */
public class FilterForEvalEventPropMayCoerce implements FilterSpecParamInValue {
    private final String resultEventAsName;
    private final String resultEventProperty;
    private final boolean isMustCoerce;
    private final Class coercionType;
    private static final long serialVersionUID = -2806996166528183416L;

    /**
     * Ctor.
     *
     * @param resultEventAsName   is the event tag
     * @param resultEventProperty is the event property name
     * @param isMustCoerce        indicates on whether numeric coercion must be performed
     * @param coercionType        indicates the numeric coercion type to use
     */
    public FilterForEvalEventPropMayCoerce(String resultEventAsName, String resultEventProperty, boolean isMustCoerce, Class coercionType) {
        this.resultEventAsName = resultEventAsName;
        this.resultEventProperty = resultEventProperty;
        this.coercionType = coercionType;
        this.isMustCoerce = isMustCoerce;
    }

    public Class getReturnType() {
        return coercionType;
    }

    public boolean constant() {
        return false;
    }

    public final Object getFilterValue(MatchedEventMap matchedEvents, ExprEvaluatorContext evaluatorContext) {
        EventBean theEvent = matchedEvents.getMatchingEventByTag(resultEventAsName);
        if (theEvent == null) {
            throw new IllegalStateException("Matching event named " +
                    '\'' + resultEventAsName + "' not found in event result set");
        }

        Object value = theEvent.get(resultEventProperty);

        // Coerce if necessary
        if (isMustCoerce) {
            if (value != null) {
                value = JavaClassHelper.coerceBoxed((Number) value, coercionType);
            }
        }
        return value;
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

        if (!(obj instanceof FilterForEvalEventPropMayCoerce)) {
            return false;
        }

        FilterForEvalEventPropMayCoerce other = (FilterForEvalEventPropMayCoerce) obj;
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
