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
package com.espertech.esper.filterspec;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprFilterSpecLookupable;
import com.espertech.esper.util.SimpleNumberCoercer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;

/**
 * This class represents a filter parameter containing a reference to another event's property
 * in the event pattern result, for use to describe a filter parameter in a {@link FilterSpecCompiled} filter specification.
 */
public final class FilterSpecParamEventPropIndexed extends FilterSpecParam {
    private static final Logger log = LoggerFactory.getLogger(FilterSpecParamEventPropIndexed.class);
    private final String resultEventAsName;
    private final int resultEventIndex;
    private final String resultEventProperty;
    private final boolean isMustCoerce;
    private final transient SimpleNumberCoercer numberCoercer;
    private final Class coercionType;
    private final String statementName;
    private static final long serialVersionUID = -1781904301744323795L;

    /**
     * Constructor.
     *
     * @param lookupable          is the lookupable
     * @param filterOperator      is the type of compare
     * @param resultEventAsName   is the name of the result event from which to get a property value to compare
     * @param resultEventProperty is the name of the property to get from the named result event
     * @param isMustCoerce        indicates on whether numeric coercion must be performed
     * @param coercionType        indicates the numeric coercion type to use
     * @param numberCoercer       interface to use to perform coercion
     * @param resultEventIndex    index
     * @param statementName       statement name
     * @throws IllegalArgumentException if an operator was supplied that does not take a single constant value
     */
    public FilterSpecParamEventPropIndexed(ExprFilterSpecLookupable lookupable, FilterOperator filterOperator, String resultEventAsName,
                                           int resultEventIndex, String resultEventProperty, boolean isMustCoerce,
                                           SimpleNumberCoercer numberCoercer, Class coercionType, String statementName)
            throws IllegalArgumentException {
        super(lookupable, filterOperator);
        this.resultEventAsName = resultEventAsName;
        this.resultEventIndex = resultEventIndex;
        this.resultEventProperty = resultEventProperty;
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

    public Object getFilterValue(MatchedEventMap matchedEvents, ExprEvaluatorContext exprEvaluatorContext, EngineImportService engineImportService, Annotation[] annotations) {
        EventBean[] events = (EventBean[]) matchedEvents.getMatchingEventAsObjectByTag(resultEventAsName);

        Object value = null;
        if (events == null) {
            if (log.isDebugEnabled()) {
                log.debug("Matching events for tag '" + resultEventAsName + "' returned a null result, using null value in filter criteria, for statement '" + statementName + "'");
            }
        } else if (resultEventIndex > (events.length - 1)) {
            log.warn("Matching events for tag '" + resultEventAsName + "' returned no result for index " + resultEventIndex + " at array length " + events.length + ", using null value in filter criteria, for statement '" + statementName + "'");
        } else {
            value = events[resultEventIndex].get(resultEventProperty);
        }

        // Coerce if necessary
        if (isMustCoerce) {
            value = numberCoercer.coerceBoxed((Number) value);
        }
        return value;
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

        if (!(obj instanceof FilterSpecParamEventPropIndexed)) {
            return false;
        }

        FilterSpecParamEventPropIndexed other = (FilterSpecParamEventPropIndexed) obj;
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
}
