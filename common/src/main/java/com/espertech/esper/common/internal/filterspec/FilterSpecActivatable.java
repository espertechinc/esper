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
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.StatementContextFilterEvalEnv;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;

import java.io.StringWriter;
import java.util.Arrays;

/**
 * Contains the filter criteria to sift through events. The filter criteria are the event class to look for and
 * a set of parameters (attribute names, operators and constant/range values).
 */
public final class FilterSpecActivatable {
    private final EventType filterForEventType;
    private final String filterForEventTypeName;
    private final FilterSpecParam[][] parameters;
    private final PropertyEvaluator optionalPropertyEvaluator;
    private final int filterCallbackId;

    /**
     * Constructor - validates parameter list against event type, throws exception if invalid
     * property names or mismatcing filter operators are found.
     *
     * @param eventType                 is the event type
     * @param filterParameters          is a list of filter parameters
     * @param eventTypeName             is the name of the event type
     * @param optionalPropertyEvaluator optional if evaluating properties returned by filtered events
     * @param filterCallbackId          filter id
     * @throws IllegalArgumentException if validation invalid
     */
    public FilterSpecActivatable(EventType eventType, String eventTypeName, FilterSpecParam[][] filterParameters,
                                 PropertyEvaluator optionalPropertyEvaluator, int filterCallbackId) {
        this.filterForEventType = eventType;
        this.filterForEventTypeName = eventTypeName;
        this.parameters = filterParameters;
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
    public final FilterSpecParam[][] getParameters() {
        return parameters;
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
     * @return filter values
     **/
    public FilterValueSetParam[][] getValueSet(MatchedEventMap matchedEvents, FilterValueSetParam[][] addendum, ExprEvaluatorContext exprEvaluatorContext, StatementContextFilterEvalEnv filterEvalEnv) {
        FilterValueSetParam[][] valueList = evaluateValueSet(parameters, matchedEvents, exprEvaluatorContext, filterEvalEnv);
        if (addendum != null) {
            valueList = FilterAddendumUtil.multiplyAddendum(addendum, valueList);
        }
        return valueList;
    }


    public static FilterValueSetParam[][] evaluateValueSet(FilterSpecParam[][] parameters, MatchedEventMap matchedEvents, AgentInstanceContext agentInstanceContext) {
        return evaluateValueSet(parameters, matchedEvents, agentInstanceContext, agentInstanceContext.getStatementContextFilterEvalEnv());
    }

    public static FilterValueSetParam[][] evaluateValueSet(FilterSpecParam[][] parameters, MatchedEventMap matchedEvents, ExprEvaluatorContext exprEvaluatorContext, StatementContextFilterEvalEnv filterEvalEnv) {
        FilterValueSetParam[][] valueList = new FilterValueSetParam[parameters.length][];
        for (int i = 0; i < parameters.length; i++) {
            valueList[i] = new FilterValueSetParam[parameters[i].length];
            populateValueSet(valueList[i], matchedEvents, parameters[i], exprEvaluatorContext, filterEvalEnv);
        }
        return valueList;
    }

    private static void populateValueSet(FilterValueSetParam[] valueList, MatchedEventMap matchedEvents, FilterSpecParam[] specParams, ExprEvaluatorContext exprEvaluatorContext, StatementContextFilterEvalEnv filterEvalEnv) {
        // Ask each filter specification parameter for the actual value to filter for
        int count = 0;
        for (FilterSpecParam specParam : specParams) {
            Object filterForValue = specParam.getFilterValue(matchedEvents, exprEvaluatorContext, filterEvalEnv);
            FilterValueSetParam valueParam = new FilterValueSetParamImpl(specParam.getLookupable(), specParam.getFilterOperator(), filterForValue);
            valueList[count] = valueParam;
            count++;
        }
    }

    @SuppressWarnings({"StringConcatenationInsideStringBufferAppend"})
    public final String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("FilterSpecActivatable type=" + this.filterForEventType);
        buffer.append(" parameters=" + Arrays.toString(parameters));
        return buffer.toString();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof FilterSpecActivatable)) {
            return false;
        }

        FilterSpecActivatable other = (FilterSpecActivatable) obj;
        if (!equalsTypeAndFilter(other)) {
            return false;
        }

        if ((this.optionalPropertyEvaluator == null) && (other.optionalPropertyEvaluator == null)) {
            return true;
        }
        if ((this.optionalPropertyEvaluator != null) && (other.optionalPropertyEvaluator == null)) {
            return false;
        }
        if ((this.optionalPropertyEvaluator == null) && (other.optionalPropertyEvaluator != null)) {
            return false;
        }

        return this.optionalPropertyEvaluator.compareTo(other.optionalPropertyEvaluator);
    }

    /**
     * Compares only the type and filter portion and not the property evaluation portion.
     *
     * @param other filter to compare
     * @return true if same
     */
    public boolean equalsTypeAndFilter(FilterSpecActivatable other) {
        if (this.filterForEventType != other.filterForEventType) {
            return false;
        }
        if (this.parameters.length != other.parameters.length) {
            return false;
        }

        for (int i = 0; i < this.parameters.length; i++) {
            FilterSpecParam[] lineThis = this.parameters[i];
            FilterSpecParam[] lineOther = other.parameters[i];
            if (lineThis.length != lineOther.length) {
                return false;
            }

            for (int j = 0; j < lineThis.length; j++) {
                if (!lineThis[j].equals(lineOther[j])) {
                    return false;
                }
            }
        }
        return true;
    }

    public int hashCode() {
        int hashCode = filterForEventType.hashCode();
        for (FilterSpecParam[] paramLine : parameters) {
            for (FilterSpecParam param : paramLine) {
                hashCode ^= 31 * param.hashCode();
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
        if (getParameters() != null && getParameters().length > 0) {
            writer.write('(');
            String delimiter = "";
            for (FilterSpecParam[] paramLine : getParameters()) {
                writer.write(delimiter);
                writeFilter(writer, paramLine);
                delimiter = " or ";
            }
            writer.write(')');
        }
        return writer.toString();
    }

    private static void writeFilter(StringWriter writer, FilterSpecParam[] paramLine) {
        String delimiter = "";
        for (FilterSpecParam param : paramLine) {
            writer.write(delimiter);
            writer.write(param.getLookupable().getExpression());
            writer.write(param.getFilterOperator().getTextualOp());
            writer.write("...");
            delimiter = ",";
        }
    }
}
