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
import com.espertech.esper.filterspec.FilterSpecParamFilterForEvalDouble;
import com.espertech.esper.filterspec.MatchedEventMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An event property as a filter parameter representing a range.
 */
public class FilterForEvalEventPropIndexedDouble implements FilterSpecParamFilterForEvalDouble {
    private static final Logger log = LoggerFactory.getLogger(FilterForEvalEventPropIndexedDouble.class);
    private final String resultEventAsName;
    private final int resultEventIndex;
    private final String resultEventProperty;
    private final String statementName;
    private static final long serialVersionUID = -2443484252813342579L;

    /**
     * Ctor.
     *
     * @param resultEventAsName   is the event tag
     * @param resultEventProperty is the event property name
     * @param resultEventIndex    index for event
     * @param statementName       statement name
     */
    public FilterForEvalEventPropIndexedDouble(String resultEventAsName, int resultEventIndex, String resultEventProperty, String statementName) {
        this.resultEventAsName = resultEventAsName;
        this.resultEventIndex = resultEventIndex;
        this.resultEventProperty = resultEventProperty;
        this.statementName = statementName;
    }

    public final Double getFilterValue(MatchedEventMap matchedEvents, ExprEvaluatorContext exprEvaluatorContext) {
        EventBean[] events = (EventBean[]) matchedEvents.getMatchingEventAsObjectByTag(resultEventAsName);

        Number value;
        if (events == null) {
            if (log.isDebugEnabled()) {
                log.debug("Matching events for tag '" + resultEventAsName + "' returned a null result, using null value in filter criteria, for statement '" + statementName + "'");
            }
            return null;
        } else if (resultEventIndex > (events.length - 1)) {
            log.warn("Matching events for tag '" + resultEventAsName + "' returned no result for index " + resultEventIndex + " at array length " + events.length + ", using null value in filter criteria, for statement '" + statementName + "'");
            return null;
        } else {
            value = (Number) events[resultEventIndex].get(resultEventProperty);
        }

        if (value == null) {
            return null;
        }
        return value.doubleValue();
    }

    public Double getFilterValueDouble(MatchedEventMap matchedEvents, ExprEvaluatorContext exprEvaluatorContext) {
        return getFilterValue(matchedEvents, exprEvaluatorContext);
    }

    public final String toString() {
        return "resultEventProp=" + resultEventAsName + '.' + resultEventProperty;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof FilterForEvalEventPropIndexedDouble)) {
            return false;
        }

        FilterForEvalEventPropIndexedDouble other = (FilterForEvalEventPropIndexedDouble) obj;
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
