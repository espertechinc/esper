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

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.lang.annotation.Annotation;
import java.util.*;

/**
 * Contains the filter criteria to sift through events. The filter criteria are the event class to look for and
 * a set of parameters (attribute names, operators and constant/range values).
 */
public final class FilterSpecCompiled {
    private final static FilterSpecParamComparator COMPARATOR_PARAMETERS = new FilterSpecParamComparator();

    private final EventType filterForEventType;
    private final String filterForEventTypeName;
    private final FilterSpecParam[][] parameters;
    private final PropertyEvaluator optionalPropertyEvaluator;

    /**
     * Constructor - validates parameter list against event type, throws exception if invalid
     * property names or mismatcing filter operators are found.
     *
     * @param eventType                 is the event type
     * @param filterParameters          is a list of filter parameters
     * @param eventTypeName             is the name of the event type
     * @param optionalPropertyEvaluator optional if evaluating properties returned by filtered events
     * @throws IllegalArgumentException if validation invalid
     */
    public FilterSpecCompiled(EventType eventType, String eventTypeName, List<FilterSpecParam>[] filterParameters,
                              PropertyEvaluator optionalPropertyEvaluator) {
        this.filterForEventType = eventType;
        this.filterForEventTypeName = eventTypeName;
        this.parameters = sortRemoveDups(filterParameters);
        this.optionalPropertyEvaluator = optionalPropertyEvaluator;
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
     * @param matchedEvents        contains the result events to use for determining filter values
     * @param addendum             context addendum
     * @param exprEvaluatorContext
     * @param engineImportService
     * @param annotations @return filter values    */
    public FilterValueSet getValueSet(MatchedEventMap matchedEvents, FilterValueSetParam[][] addendum, ExprEvaluatorContext exprEvaluatorContext, EngineImportService engineImportService, Annotation[] annotations) {
        FilterValueSetParam[][] valueList = new FilterValueSetParam[parameters.length][];
        for (int i = 0; i < parameters.length; i++) {
            valueList[i] = new FilterValueSetParam[parameters[i].length];
            populateValueSet(valueList[i], matchedEvents, parameters[i], exprEvaluatorContext, engineImportService, annotations);
        }

        if (addendum != null) {
            valueList = FilterAddendumUtil.multiplyAddendum(addendum, valueList);
        }
        return new FilterValueSetImpl(filterForEventType, valueList);
    }

    private static void populateValueSet(FilterValueSetParam[] valueList, MatchedEventMap matchedEvents, FilterSpecParam[] specParams, ExprEvaluatorContext exprEvaluatorContext, EngineImportService engineImportService, Annotation[] annotations) {
        // Ask each filter specification parameter for the actual value to filter for
        int count = 0;
        for (FilterSpecParam specParam : specParams) {
            Object filterForValue = specParam.getFilterValue(matchedEvents, exprEvaluatorContext, engineImportService, annotations);
            FilterValueSetParam valueParam = new FilterValueSetParamImpl(specParam.getLookupable(), specParam.getFilterOperator(), filterForValue);
            valueList[count] = valueParam;
            count++;
        }
    }

    @SuppressWarnings({"StringConcatenationInsideStringBufferAppend"})
    public final String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("FilterSpecCompiled type=" + this.filterForEventType);
        buffer.append(" parameters=" + Arrays.toString(parameters));
        return buffer.toString();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof FilterSpecCompiled)) {
            return false;
        }

        FilterSpecCompiled other = (FilterSpecCompiled) obj;
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
    public boolean equalsTypeAndFilter(FilterSpecCompiled other) {
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

    public int getFilterSpecIndexAmongAll(FilterSpecCompiled[] filterSpecAll) {
        for (int i = 0; i < filterSpecAll.length; i++) {
            if (this == filterSpecAll[i]) {
                return i;
            }
        }
        throw new EPException("Failed to find find filter spec among list of known filters");
    }

    protected static FilterSpecParam[][] sortRemoveDups(List<FilterSpecParam>[] parameters) {
        FilterSpecParam[][] processed = new FilterSpecParam[parameters.length][];
        for (int i = 0; i < parameters.length; i++) {
            processed[i] = sortRemoveDups(parameters[i]);
        }
        return processed;
    }

    protected static FilterSpecParam[] sortRemoveDups(List<FilterSpecParam> parameters) {

        if (parameters.isEmpty()) {
            return FilterSpecParam.EMPTY_PARAM_ARRAY;
        }

        if (parameters.size() == 1) {
            return new FilterSpecParam[]{parameters.get(0)};
        }

        ArrayDeque<FilterSpecParam> result = new ArrayDeque<FilterSpecParam>();
        TreeMap<FilterOperator, List<FilterSpecParam>> map = new TreeMap<FilterOperator, List<FilterSpecParam>>(COMPARATOR_PARAMETERS);
        for (FilterSpecParam parameter : parameters) {

            List<FilterSpecParam> list = map.get(parameter.getFilterOperator());
            if (list == null) {
                list = new ArrayList<FilterSpecParam>();
                map.put(parameter.getFilterOperator(), list);
            }

            boolean hasDuplicate = false;
            for (FilterSpecParam existing : list) {
                if (existing.getLookupable().equals(parameter.getLookupable())) {
                    hasDuplicate = true;
                    break;
                }
            }
            if (hasDuplicate) {
                continue;
            }

            list.add(parameter);
        }

        for (Map.Entry<FilterOperator, List<FilterSpecParam>> entry : map.entrySet()) {
            result.addAll(entry.getValue());
        }
        return FilterSpecParam.toArray(result);
    }
}
