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

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.compile.stage2.FilterSpecPlan;
import com.espertech.esper.common.internal.compile.stage2.FilterSpecPlanPath;
import com.espertech.esper.common.internal.compile.stage2.FilterSpecPlanPathTriplet;
import com.espertech.esper.common.internal.context.util.StatementContextFilterEvalEnv;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;

import java.io.StringWriter;

/**
 * Contains the filter criteria to sift through events. The filter criteria are the event class to look for and
 * a set of parameters (attribute names, operators and constant/range values).
 */
public final class FilterSpecActivatable {
    public final static EPTypeClass EPTYPE = new EPTypeClass(FilterSpecActivatable.class);

    private final EventType filterForEventType;
    private final String filterForEventTypeName;
    private final FilterSpecPlan plan;
    private final PropertyEvaluator optionalPropertyEvaluator;
    private final int filterCallbackId;

    /**
     * Constructor - validates parameter list against event type, throws exception if invalid
     * property names or mismatcing filter operators are found.
     *
     * @param eventType                 is the event type
     * @param plan                      plan is a list of filter parameters, i.e. paths and triplets
     * @param eventTypeName             is the name of the event type
     * @param optionalPropertyEvaluator optional if evaluating properties returned by filtered events
     * @param filterCallbackId          filter id
     * @throws IllegalArgumentException if validation invalid
     */
    public FilterSpecActivatable(EventType eventType, String eventTypeName, FilterSpecPlan plan,
                                 PropertyEvaluator optionalPropertyEvaluator, int filterCallbackId) {
        this.filterForEventType = eventType;
        this.filterForEventTypeName = eventTypeName;
        this.plan = plan;
        this.optionalPropertyEvaluator = optionalPropertyEvaluator;
        if (filterCallbackId == -1) {
            throw new IllegalArgumentException("Filter callback id is unassigned");
        }
        this.filterCallbackId = filterCallbackId;
    }

    /**
     * Returns type of event to filter for.
     *
     * @return event type
     */
    public final EventType getFilterForEventType() {
        return filterForEventType;
    }

    /**
     * Returns list of filter parameters.
     *
     * @return list of filter params
     */
    public final FilterSpecPlan getPlan() {
        return plan;
    }

    /**
     * Returns the event type name.
     *
     * @return event type name
     */
    public String getFilterForEventTypeName() {
        return filterForEventTypeName;
    }

    /**
     * Return the evaluator for property value if any is attached, or none if none attached.
     *
     * @return property evaluator
     */
    public PropertyEvaluator getOptionalPropertyEvaluator() {
        return optionalPropertyEvaluator;
    }

    /**
     * Returns the result event type of the filter specification.
     *
     * @return event type
     */
    public EventType getResultEventType() {
        if (optionalPropertyEvaluator != null) {
            return optionalPropertyEvaluator.getFragmentEventType();
        } else {
            return filterForEventType;
        }
    }

    /**
     * Returns the values for the filter, using the supplied result events to ask filter parameters
     * for the value to filter for.
     *
     * @param matchedEvents        contains the result events to use for determining filter values
     * @param addendum             context addendum
     * @param exprEvaluatorContext context
     * @param filterEvalEnv        env
     * @return filter values, or null when negated
     **/
    public FilterValueSetParam[][] getValueSet(MatchedEventMap matchedEvents, FilterValueSetParam[][] addendum, ExprEvaluatorContext exprEvaluatorContext, StatementContextFilterEvalEnv filterEvalEnv) {
        FilterValueSetParam[][] valueList = plan.evaluateValueSet(matchedEvents, exprEvaluatorContext, filterEvalEnv);
        if (addendum != null) {
            valueList = FilterAddendumUtil.multiplyAddendum(addendum, valueList);
        }
        return valueList;
    }

    @SuppressWarnings({"StringConcatenationInsideStringBufferAppend"})
    public final String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("FilterSpecActivatable type=" + this.filterForEventType);
        buffer.append(" parameters=" + plan.toString());
        return buffer.toString();
    }

    public boolean equals(Object obj) {
        return this == obj;
    }

    public int hashCode() {
        int hashCode = filterForEventType.hashCode();
        for (FilterSpecPlanPath path : plan.getPaths()) {
            for (FilterSpecPlanPathTriplet triplet : path.getTriplets()) {
                hashCode ^= 31 * triplet.getParam().hashCode();
            }
        }
        return hashCode;
    }

    public int getFilterCallbackId() {
        return filterCallbackId;
    }

    public String getFilterText() {
        StringWriter writer = new StringWriter();
        writer.write(getFilterForEventType().getName());
        if (getPlan().getPaths() != null && getPlan().getPaths().length > 0) {
            writer.write('(');
            String delimiter = "";
            for (FilterSpecPlanPath path : getPlan().getPaths()) {
                writer.write(delimiter);
                writeFilter(writer, path);
                delimiter = " or ";
            }
            writer.write(')');
        }
        return writer.toString();
    }

    private static void writeFilter(StringWriter writer, FilterSpecPlanPath path) {
        String delimiter = "";
        for (FilterSpecPlanPathTriplet triplet : path.getTriplets()) {
            writer.write(delimiter);
            writer.write(triplet.getParam().getLkupable().getExpression());
            writer.write(triplet.getParam().getFilterOperator().getTextualOp());
            writer.write("...");
            delimiter = ",";
        }
    }
}
