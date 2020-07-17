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
import com.espertech.esper.common.internal.filterspec.FilterOperator;
import com.espertech.esper.common.internal.filterspec.FilterSpecActivatable;
import com.espertech.esper.common.internal.filterspec.FilterSpecParamExprNodeForge;

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
    private final FilterSpecPlanForge parameters;
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
    public FilterSpecCompiled(EventType eventType, String eventTypeName, FilterSpecPlanForge filterParameters,
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
    public final FilterSpecPlanForge getParameters() {
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
        buffer.append(" parameters=" + parameters.toString());
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
        return parameters.equalsFilter(other.parameters);
    }

    public int hashCode() {
        int hashCode = filterForEventType.hashCode();
        for (FilterSpecPlanPathForge path : parameters.getPaths()) {
            for (FilterSpecPlanPathTripletForge triplet : path.getTriplets()) {
                hashCode ^= 31 * triplet.hashCode();
            }
        }
        return hashCode;
    }

    protected static FilterSpecPlanForge sortRemoveDups(FilterSpecPlanForge parameters) {
        FilterSpecPlanPathForge[] processed = new FilterSpecPlanPathForge[parameters.getPaths().length];
        for (int i = 0; i < parameters.getPaths().length; i++) {
            processed[i] = sortRemoveDups(parameters.getPaths()[i]);
        }
        return new FilterSpecPlanForge(processed, parameters.getFilterConfirm(), parameters.getFilterNegate(), parameters.getConvertorForge());
    }

    protected static FilterSpecPlanPathForge sortRemoveDups(FilterSpecPlanPathForge parameters) {

        if (parameters.getTriplets().length <= 1) {
            return parameters;
        }

        ArrayDeque<FilterSpecPlanPathTripletForge> result = new ArrayDeque<>();
        TreeMap<FilterOperator, List<FilterSpecPlanPathTripletForge>> map = new TreeMap<>(COMPARATOR_PARAMETERS);
        for (FilterSpecPlanPathTripletForge parameter : parameters.getTriplets()) {

            List<FilterSpecPlanPathTripletForge> list = map.get(parameter.getParam().getFilterOperator());
            if (list == null) {
                list = new ArrayList<>();
                map.put(parameter.getParam().getFilterOperator(), list);
            }

            boolean hasDuplicate = false;
            for (FilterSpecPlanPathTripletForge existing : list) {
                if (existing.getParam().getLookupable().equals(parameter.getParam().getLookupable())) {
                    hasDuplicate = true;
                    break;
                }
            }
            if (hasDuplicate) {
                continue;
            }

            list.add(parameter);
        }

        for (Map.Entry<FilterOperator, List<FilterSpecPlanPathTripletForge>> entry : map.entrySet()) {
            result.addAll(entry.getValue());
        }
        FilterSpecPlanPathTripletForge[] triplets = result.toArray(new FilterSpecPlanPathTripletForge[0]);
        return new FilterSpecPlanPathForge(triplets, parameters.getPathNegate());
    }

    public CodegenMethod makeCodegen(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(FilterSpecActivatable.EPTYPE, FilterSpecCompiled.class, classScope);

        if (filterCallbackId == -1) {
            throw new IllegalStateException("Unassigned filter callback id");
        }

        CodegenExpression propertyEval = optionalPropertyEvaluator == null ? constantNull() : optionalPropertyEvaluator.make(method, symbols, classScope);
        method.getBlock()
                .declareVar(EventType.EPTYPE, "eventType", EventTypeUtility.resolveTypeCodegen(filterForEventType, EPStatementInitServices.REF))
                .declareVar(FilterSpecPlan.EPTYPE, "plan", parameters.codegenWithEventType(method, ref("eventType"), symbols.getAddInitSvc(method), classScope))
                .declareVar(FilterSpecActivatable.EPTYPE, "activatable", newInstance(FilterSpecActivatable.EPTYPE, SAIFFInitializeSymbolWEventType.REF_EVENTTYPE,
                        constant(filterForEventType.getName()), ref("plan"), propertyEval, constant(filterCallbackId)))
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
        for (FilterSpecPlanPathForge path : parameters.getPaths()) {
            for (FilterSpecPlanPathTripletForge triplet : path.getTriplets()) {
                if (triplet.getParam() instanceof FilterSpecParamExprNodeForge) {
                    consumer.accept((FilterSpecParamExprNodeForge) triplet.getParam());
                }
            }
        }
    }
}
