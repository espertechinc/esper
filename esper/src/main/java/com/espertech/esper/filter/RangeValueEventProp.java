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
import com.espertech.esper.pattern.MatchedEventMap;

/**
 * An event property as a filter parameter representing a range.
 */
public class RangeValueEventProp implements FilterSpecParamRangeValue {
    private final String resultEventAsName;
    private final String resultEventProperty;
    private static final long serialVersionUID = 3372433556541547809L;

    /**
     * Ctor.
     *
     * @param resultEventAsName   is the event tag
     * @param resultEventProperty is the event property name
     */
    public RangeValueEventProp(String resultEventAsName, String resultEventProperty) {
        this.resultEventAsName = resultEventAsName;
        this.resultEventProperty = resultEventProperty;
    }

    public Object getFilterValue(MatchedEventMap matchedEvents, ExprEvaluatorContext exprEvaluatorContext) {
        EventBean theEvent = matchedEvents.getMatchingEventByTag(resultEventAsName);
        if (theEvent == null) {
            throw new IllegalStateException("Matching event named " +
                    '\'' + resultEventAsName + "' not found in event result set");
        }

        Number value = (Number) theEvent.get(resultEventProperty);
        if (value == null) {
            return null;
        }
        return value.doubleValue();
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

        if (!(obj instanceof RangeValueEventProp)) {
            return false;
        }

        RangeValueEventProp other = (RangeValueEventProp) obj;
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
