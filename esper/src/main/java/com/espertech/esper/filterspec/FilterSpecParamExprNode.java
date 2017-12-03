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

import com.espertech.esper.client.ConfigurationEngineDefaults;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.ExprFilterSpecLookupable;
import com.espertech.esper.epl.table.mgmt.TableService;
import com.espertech.esper.epl.variable.VariableService;
import com.espertech.esper.event.EventAdapterService;

import java.lang.annotation.Annotation;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This class represents an arbitrary expression node returning a boolean value as a filter parameter in an {@link FilterSpecCompiled} filter specification.
 */
public final class FilterSpecParamExprNode extends FilterSpecParam {
    private final ExprNode exprNode;
    private final LinkedHashMap<String, Pair<EventType, String>> taggedEventTypes;
    private final LinkedHashMap<String, Pair<EventType, String>> arrayEventTypes;
    private final transient EventAdapterService eventAdapterService;
    private final transient FilterBooleanExpressionFactory filterBooleanExpressionFactory;
    private final transient VariableService variableService;
    private final transient TableService tableService;
    private final boolean hasVariable;
    private final boolean useLargeThreadingProfile;
    private final boolean hasFilterStreamSubquery;
    private final boolean hasTableAccess;

    private int filterSpecId;
    private int filterSpecParamPathNum;

    private static final long serialVersionUID = 2298436088557677833L;

    public FilterSpecParamExprNode(ExprFilterSpecLookupable lookupable,
                                   FilterOperator filterOperator,
                                   ExprNode exprNode,
                                   LinkedHashMap<String, Pair<EventType, String>> taggedEventTypes,
                                   LinkedHashMap<String, Pair<EventType, String>> arrayEventTypes,
                                   VariableService variableService,
                                   TableService tableService,
                                   EventAdapterService eventAdapterService,
                                   FilterBooleanExpressionFactory filterBooleanExpressionFactory,
                                   ConfigurationEngineDefaults.ThreadingProfile threadingProfile,
                                   boolean hasSubquery,
                                   boolean hasTableAccess,
                                   boolean hasVariable)
            throws IllegalArgumentException {
        super(lookupable, filterOperator);
        if (filterOperator != FilterOperator.BOOLEAN_EXPRESSION) {
            throw new IllegalArgumentException("Invalid filter operator for filter expression node");
        }
        this.exprNode = exprNode;
        this.taggedEventTypes = taggedEventTypes;
        this.arrayEventTypes = arrayEventTypes;
        this.variableService = variableService;
        this.tableService = tableService;
        this.eventAdapterService = eventAdapterService;
        this.filterBooleanExpressionFactory = filterBooleanExpressionFactory;
        this.useLargeThreadingProfile = threadingProfile == ConfigurationEngineDefaults.ThreadingProfile.LARGE;
        this.hasFilterStreamSubquery = hasSubquery;
        this.hasTableAccess = hasTableAccess;
        this.hasVariable = hasVariable;
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

    public final ExprNodeAdapterBase getFilterValue(MatchedEventMap matchedEvents, ExprEvaluatorContext exprEvaluatorContext, EngineImportService engineImportService, Annotation[] annotations) {
        EventBean[] events = null;

        if ((taggedEventTypes != null && !taggedEventTypes.isEmpty()) || (arrayEventTypes != null && !arrayEventTypes.isEmpty())) {
            int size = 0;
            size += (taggedEventTypes != null) ? taggedEventTypes.size() : 0;
            size += (arrayEventTypes != null) ? arrayEventTypes.size() : 0;
            events = new EventBean[size + 1];

            int count = 1;
            if (taggedEventTypes != null) {
                for (String tag : taggedEventTypes.keySet()) {
                    events[count] = matchedEvents.getMatchingEventByTag(tag);
                    count++;
                }
            }

            if (arrayEventTypes != null) {
                for (Map.Entry<String, Pair<EventType, String>> entry : arrayEventTypes.entrySet()) {
                    EventType compositeEventType = entry.getValue().getFirst();
                    events[count] = eventAdapterService.adapterForTypedMap(matchedEvents.getMatchingEventsAsMap(), compositeEventType);
                    count++;
                }
            }
        }

        return filterBooleanExpressionFactory.make(this, events, exprEvaluatorContext, exprEvaluatorContext.getAgentInstanceId(), engineImportService, annotations);
    }

    public final String toString() {
        return super.toString() + "  exprNode=" + exprNode.toString();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof FilterSpecParamExprNode)) {
            return false;
        }

        FilterSpecParamExprNode other = (FilterSpecParamExprNode) obj;
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

    public int getFilterSpecId() {
        return filterSpecId;
    }

    public void setFilterSpecId(int filterSpecId) {
        this.filterSpecId = filterSpecId;
    }

    public int getFilterSpecParamPathNum() {
        return filterSpecParamPathNum;
    }

    public void setFilterSpecParamPathNum(int filterSpecParamPathNum) {
        this.filterSpecParamPathNum = filterSpecParamPathNum;
    }

    public LinkedHashMap<String, Pair<EventType, String>> getArrayEventTypes() {
        return arrayEventTypes;
    }

    public EventAdapterService getEventAdapterService() {
        return eventAdapterService;
    }

    public FilterBooleanExpressionFactory getFilterBooleanExpressionFactory() {
        return filterBooleanExpressionFactory;
    }

    public VariableService getVariableService() {
        return variableService;
    }

    public TableService getTableService() {
        return tableService;
    }

    public boolean isHasVariable() {
        return hasVariable;
    }

    public boolean isUseLargeThreadingProfile() {
        return useLargeThreadingProfile;
    }

    public boolean isHasFilterStreamSubquery() {
        return hasFilterStreamSubquery;
    }

    public boolean isHasTableAccess() {
        return hasTableAccess;
    }
}
