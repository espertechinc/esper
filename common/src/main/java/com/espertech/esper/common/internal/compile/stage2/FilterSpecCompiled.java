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
package com.espertech.esper.common.internal.compile.stage2;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbolWEventType;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.contained.PropertyEvaluatorForge;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.filterspec.*;

import java.util.*;
import java.util.function.Consumer;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Contains the filter criteria to sift through events. The filter criteria are the event class to look for and
 * a set of parameters (attribute names, operators and constant/range values).
 */
public final class FilterSpecCompiled {
    private final static FilterSpecParamComparator COMPARATOR_PARAMETERS = new FilterSpecParamComparator();

    private final EventType filterForEventType;
    private final String filterForEventTypeName;
    private final FilterSpecParamForge[][] parameters;
    private final PropertyEvaluatorForge optionalPropertyEvaluator;
    private int filterCallbackId = -1;

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
    public FilterSpecCompiled(EventType eventType, String eventTypeName, List<FilterSpecParamForge>[] filterParameters,
                              PropertyEvaluatorForge optionalPropertyEvaluator) {
        this.filterForEventType = eventType;
        this.filterForEventTypeName = eventTypeName;
        this.parameters = sortRemoveDups(filterParameters);
        this.optionalPropertyEvaluator = optionalPropertyEvaluator;
    }

    public void setFilterCallbackId(int filterCallbackId) {
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
    public final FilterSpecParamForge[][] getParameters() {
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
    public PropertyEvaluatorForge getOptionalPropertyEvaluator() {
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
            FilterSpecParamForge[] lineThis = this.parameters[i];
            FilterSpecParamForge[] lineOther = other.parameters[i];
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
        for (FilterSpecParamForge[] paramLine : parameters) {
            for (FilterSpecParamForge param : paramLine) {
                hashCode ^= 31 * param.hashCode();
            }
        }
        return hashCode;
    }

    protected static FilterSpecParamForge[][] sortRemoveDups(List<FilterSpecParamForge>[] parameters) {
        FilterSpecParamForge[][] processed = new FilterSpecParamForge[parameters.length][];
        for (int i = 0; i < parameters.length; i++) {
            processed[i] = sortRemoveDups(parameters[i]);
        }
        return processed;
    }

    protected static FilterSpecParamForge[] sortRemoveDups(List<FilterSpecParamForge> parameters) {

        if (parameters.isEmpty()) {
            return FilterSpecParamForge.EMPTY_PARAM_ARRAY;
        }

        if (parameters.size() == 1) {
            return new FilterSpecParamForge[]{parameters.get(0)};
        }

        ArrayDeque<FilterSpecParamForge> result = new ArrayDeque<>();
        TreeMap<FilterOperator, List<FilterSpecParamForge>> map = new TreeMap<>(COMPARATOR_PARAMETERS);
        for (FilterSpecParamForge parameter : parameters) {

            List<FilterSpecParamForge> list = map.get(parameter.getFilterOperator());
            if (list == null) {
                list = new ArrayList<>();
                map.put(parameter.getFilterOperator(), list);
            }

            boolean hasDuplicate = false;
            for (FilterSpecParamForge existing : list) {
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

        for (Map.Entry<FilterOperator, List<FilterSpecParamForge>> entry : map.entrySet()) {
            result.addAll(entry.getValue());
        }
        return FilterSpecParamForge.toArray(result);
    }

    public CodegenMethod makeCodegen(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(FilterSpecActivatable.class, FilterSpecCompiled.class, classScope);

        if (filterCallbackId == -1) {
            throw new IllegalStateException("Unassigned filter callback id");
        }

        CodegenExpression propertyEval = optionalPropertyEvaluator == null ? constantNull() : optionalPropertyEvaluator.make(method, symbols, classScope);
        method.getBlock()
                .declareVar(EventType.class, "eventType", EventTypeUtility.resolveTypeCodegen(filterForEventType, EPStatementInitServices.REF))
                .declareVar(FilterSpecParam[][].class, "params", localMethod(FilterSpecParamForge.makeParamArrayArrayCodegen(parameters, classScope, method), ref("eventType"), symbols.getAddInitSvc(method)))
                .declareVar(FilterSpecActivatable.class, "activatable", newInstance(FilterSpecActivatable.class, SAIFFInitializeSymbolWEventType.REF_EVENTTYPE,
                        constant(filterForEventType.getName()), ref("params"), propertyEval, constant(filterCallbackId)))
                .expression(exprDotMethodChain(symbols.getAddInitSvc(method)).add(EPStatementInitServices.GETFILTERSPECACTIVATABLEREGISTRY).add("register", ref("activatable")))
                .methodReturn(ref("activatable"));

        return method;
    }

    public static List<FilterSpecParamExprNodeForge> makeExprNodeList(List<FilterSpecCompiled> filterSpecCompileds, List<FilterSpecParamExprNodeForge> additionalBooleanExpressions) {
        Set<FilterSpecParamExprNodeForge> boolExprs = new LinkedHashSet<>();
        for (FilterSpecCompiled spec : filterSpecCompileds) {
            spec.traverseFilterBooleanExpr(boolExprs::add);
        }
        boolExprs.addAll(additionalBooleanExpressions);
        return new ArrayList<>(boolExprs);
    }

    public void traverseFilterBooleanExpr(Consumer<FilterSpecParamExprNodeForge> consumer) {
        for (FilterSpecParamForge[] params : parameters) {
            for (FilterSpecParamForge param : params) {
                if (param instanceof FilterSpecParamExprNodeForge) {
                    consumer.accept((FilterSpecParamExprNodeForge) param);
                }
            }
        }
    }
}
