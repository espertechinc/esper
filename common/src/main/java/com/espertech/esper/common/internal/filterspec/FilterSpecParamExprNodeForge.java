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
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.util.ThreadingProfile;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionNewAnonymousClass;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbolWEventType;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeService;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactoryCodegenField;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.util.StringValue;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames.REF_EXPREVALCONTEXT;
import static com.espertech.esper.common.internal.filterspec.FilterSpecParam.REF_MATCHEDEVENTMAP;
import static com.espertech.esper.common.internal.filterspec.FilterSpecParam.REF_STMTCTXFILTEREVALENV;

/**
 * This class represents an arbitrary expression node returning a boolean value as a filter parameter in an {@link FilterSpecActivatable} filter specification.
 */
public final class FilterSpecParamExprNodeForge extends FilterSpecParamForge {
    private final ExprNode exprNode;
    private final LinkedHashMap<String, Pair<EventType, String>> taggedEventTypes;
    private final LinkedHashMap<String, Pair<EventType, String>> arrayEventTypes;
    private final StreamTypeService streamTypeService;
    private final boolean hasVariable;
    private final boolean hasFilterStreamSubquery;
    private final boolean hasTableAccess;
    private final StatementCompileTimeServices compileTimeServices;

    private int filterBoolExprId = -1;

    public FilterSpecParamExprNodeForge(ExprFilterSpecLookupableForge lookupable,
                                        FilterOperator filterOperator,
                                        ExprNode exprNode,
                                        LinkedHashMap<String, Pair<EventType, String>> taggedEventTypes,
                                        LinkedHashMap<String, Pair<EventType, String>> arrayEventTypes,
                                        StreamTypeService streamTypeService,
                                        boolean hasSubquery,
                                        boolean hasTableAccess,
                                        boolean hasVariable,
                                        StatementCompileTimeServices compileTimeServices)
            throws IllegalArgumentException {
        super(lookupable, filterOperator);
        if (filterOperator != FilterOperator.BOOLEAN_EXPRESSION) {
            throw new IllegalArgumentException("Invalid filter operator for filter expression node");
        }
        this.exprNode = exprNode;
        this.taggedEventTypes = taggedEventTypes;
        this.arrayEventTypes = arrayEventTypes;
        this.streamTypeService = streamTypeService;
        this.hasFilterStreamSubquery = hasSubquery;
        this.hasTableAccess = hasTableAccess;
        this.hasVariable = hasVariable;
        this.compileTimeServices = compileTimeServices;
    }

    /**
     * Returns the expression node of the boolean expression this filter parameter represents.
     *
     * @return expression node
     */
    public ExprNode getExprNode() {
        return exprNode;
    }

    /**
     * Returns the map of tag/stream names to event types that the filter expressions map use (for patterns)
     *
     * @return map
     */
    public LinkedHashMap<String, Pair<EventType, String>> getTaggedEventTypes() {
        return taggedEventTypes;
    }


    public final String toString() {
        return super.toString() + "  exprNode=" + exprNode.toString();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof FilterSpecParamExprNodeForge)) {
            return false;
        }

        FilterSpecParamExprNodeForge other = (FilterSpecParamExprNodeForge) obj;
        if (!super.equals(other)) {
            return false;
        }

        if (exprNode != other.exprNode) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + exprNode.hashCode();
        return result;
    }

    public int getFilterBoolExprId() {
        return filterBoolExprId;
    }

    public void setFilterBoolExprId(int filterBoolExprId) {
        this.filterBoolExprId = filterBoolExprId;
    }

    public CodegenMethod makeCodegen(CodegenClassScope classScope, CodegenMethodScope parent, SAIFFInitializeSymbolWEventType symbols) {
        if (filterBoolExprId == -1) {
            throw new IllegalStateException("Unassigned filter boolean expression path num");
        }

        CodegenMethod method = parent.makeChild(FilterSpecParamExprNode.class, this.getClass(), classScope);
        method.getBlock()
                .declareVar(ExprFilterSpecLookupable.class, "lookupable", localMethod(lookupable.makeCodegen(method, symbols, classScope)))
                .declareVar(FilterOperator.class, "op", enumValue(FilterOperator.class, filterOperator.name()));

        // getFilterValue-FilterSpecParamExprNode code
        CodegenExpressionNewAnonymousClass param = newAnonymousClass(method.getBlock(), FilterSpecParamExprNode.class, Arrays.asList(ref("lookupable"), ref("op")));
        CodegenMethod getFilterValue = CodegenMethod.makeParentNode(Object.class, this.getClass(), classScope).addParam(FilterSpecParam.GET_FILTER_VALUE_FP);
        param.addMethod("getFilterValue", getFilterValue);

        if ((taggedEventTypes != null && !taggedEventTypes.isEmpty()) || (arrayEventTypes != null && !arrayEventTypes.isEmpty())) {
            int size = (taggedEventTypes != null) ? taggedEventTypes.size() : 0;
            size += (arrayEventTypes != null) ? arrayEventTypes.size() : 0;
            getFilterValue.getBlock().declareVar(EventBean[].class, "events", newArrayByLength(EventBean.class, constant(size + 1)));

            int count = 1;
            if (taggedEventTypes != null) {
                for (String tag : taggedEventTypes.keySet()) {
                    getFilterValue.getBlock().assignArrayElement("events", constant(count), exprDotMethod(REF_MATCHEDEVENTMAP, "getMatchingEventByTag", constant(tag)));
                    count++;
                }
            }

            if (arrayEventTypes != null) {
                for (Map.Entry<String, Pair<EventType, String>> entry : arrayEventTypes.entrySet()) {
                    EventType compositeEventType = entry.getValue().getFirst();
                    CodegenExpressionField compositeEventTypeMember = classScope.addFieldUnshared(true, EventType.class, EventTypeUtility.resolveTypeCodegen(compositeEventType, EPStatementInitServices.REF));
                    CodegenExpressionField factory = classScope.addOrGetFieldSharable(EventBeanTypedEventFactoryCodegenField.INSTANCE);
                    CodegenExpression matchingAsMap = exprDotMethod(REF_MATCHEDEVENTMAP, "getMatchingEventsAsMap");
                    CodegenExpression mapBean = exprDotMethod(factory, "adapterForTypedMap", matchingAsMap, compositeEventTypeMember);
                    getFilterValue.getBlock().assignArrayElement("events", constant(count), mapBean);
                    count++;
                }
            }
        } else {
            getFilterValue.getBlock().declareVar(EventBean[].class, "events", constantNull());
        }

        getFilterValue.getBlock()
                .methodReturn(exprDotMethod(ref("filterBooleanExpressionFactory"), "make",
                        ref("this"), // FilterSpecParamExprNode filterSpecParamExprNode
                        ref("events"), // EventBean[] events
                        REF_EXPREVALCONTEXT, // ExprEvaluatorContext exprEvaluatorContext
                        exprDotMethod(REF_EXPREVALCONTEXT, "getAgentInstanceId"), // int agentInstanceId
                        REF_STMTCTXFILTEREVALENV));

        // expression evaluator
        CodegenExpressionNewAnonymousClass evaluator = ExprNodeUtilityCodegen.codegenEvaluatorNoCoerce(exprNode.getForge(), method, this.getClass(), classScope);

        // setter calls
        method.getBlock()
                .declareVar(FilterSpecParamExprNode.class, "node", param)
                .exprDotMethod(ref("node"), "setExprText", constant(StringValue.stringDelimitedTo60Char(ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(exprNode))))
                .exprDotMethod(ref("node"), "setExprNode", evaluator)
                .exprDotMethod(ref("node"), "setHasVariable", constant(hasVariable))
                .exprDotMethod(ref("node"), "setHasFilterStreamSubquery", constant(hasFilterStreamSubquery))
                .exprDotMethod(ref("node"), "setFilterBoolExprId", constant(filterBoolExprId))
                .exprDotMethod(ref("node"), "setHasTableAccess", constant(hasTableAccess))
                .exprDotMethod(ref("node"), "setFilterBooleanExpressionFactory", exprDotMethodChain(symbols.getAddInitSvc(method)).add(EPStatementInitServices.GETFILTERBOOLEANEXPRESSIONFACTORY))
                .exprDotMethod(ref("node"), "setUseLargeThreadingProfile", constant(compileTimeServices.getConfiguration().getCommon().getExecution().getThreadingProfile() == ThreadingProfile.LARGE));

        if ((taggedEventTypes != null && !taggedEventTypes.isEmpty()) || (arrayEventTypes != null && !arrayEventTypes.isEmpty())) {
            int size = (taggedEventTypes != null) ? taggedEventTypes.size() : 0;
            size += (arrayEventTypes != null) ? arrayEventTypes.size() : 0;
            method.getBlock().declareVar(EventType[].class, "providedTypes", newArrayByLength(EventType.class, constant(size + 1)));
            for (int i = 1; i < streamTypeService.getStreamNames().length; i++) {
                String tag = streamTypeService.getStreamNames()[i];
                EventType eventType = findMayNull(tag, taggedEventTypes);
                if (eventType == null) {
                    eventType = findMayNull(tag, arrayEventTypes);
                }
                if (eventType == null) {
                    throw new IllegalStateException("Failed to find event type for tag '" + tag + "'");
                }
                method.getBlock().assignArrayElement("providedTypes", constant(i), EventTypeUtility.resolveTypeCodegen(eventType, EPStatementInitServices.REF));
                // note: we leave index zero at null as that is the current event itself
            }
            method.getBlock().exprDotMethod(ref("node"), "setEventTypesProvidedBy", ref("providedTypes"));
        }

        // register boolean expression so it can be found
        method.getBlock().expression(exprDotMethodChain(symbols.getAddInitSvc(method)).add(EPStatementInitServices.GETFILTERSHAREDBOOLEXPRREGISTERY).add("registerBoolExpr", ref("node")));

        method.getBlock().methodReturn(ref("node"));
        return method;
    }

    private EventType findMayNull(String tag, LinkedHashMap<String, Pair<EventType, String>> tags) {
        if (tags == null || !tags.containsKey(tag)) {
            return null;
        }
        return tags.get(tag).getFirst();
    }
}
