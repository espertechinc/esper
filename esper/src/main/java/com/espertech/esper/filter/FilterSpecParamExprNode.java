/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.filter;

import com.espertech.esper.client.ConfigurationEngineDefaults;
import com.espertech.esper.client.ConfigurationInformation;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.core.service.ExprEvaluatorContextWTableAccess;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.visitor.ExprNodeVariableVisitor;
import com.espertech.esper.epl.table.mgmt.TableService;
import com.espertech.esper.epl.variable.VariableService;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.pattern.MatchedEventMap;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This class represents an arbitrary expression node returning a boolean value as a filter parameter in an {@link FilterSpecCompiled} filter specification.
 */
public final class FilterSpecParamExprNode extends FilterSpecParam
{
    private final ExprNode exprNode;
    private final LinkedHashMap<String, Pair<EventType, String>> taggedEventTypes;
    private final LinkedHashMap<String, Pair<EventType, String>> arrayEventTypes;
    private final transient EventAdapterService eventAdapterService;
    private final transient VariableService variableService;
    private final transient TableService tableService;
    private final boolean hasVariable;
    private final boolean useLargeThreadingProfile;
    private final boolean hasFilterStreamSubquery;
    private final boolean hasTableAccess;

    private int filterSpecId;
    private int filterSpecParamPathNum;

    private static final long serialVersionUID = 2298436088557677833L;

    /**
     * Ctor.
     * @param lookupable is the lookup-able
     * @param filterOperator is expected to be the BOOLEAN_EXPR operator
     * @param exprNode represents the boolean expression
     * @param taggedEventTypes is null if the expression doesn't need other streams, or is filled with a ordered list of stream names and types
     * @param arrayEventTypes is a map of name tags and event type per tag for repeat-expressions that generate an array of events
     * @param variableService - provides access to variables
     * @param eventAdapterService for creating event types and event beans
     * @throws IllegalArgumentException for illegal args
     */
    public FilterSpecParamExprNode(FilterSpecLookupable lookupable,
                             FilterOperator filterOperator,
                             ExprNode exprNode,
                             LinkedHashMap<String, Pair<EventType, String>> taggedEventTypes,
                             LinkedHashMap<String, Pair<EventType, String>> arrayEventTypes,
                             VariableService variableService,
                             TableService tableService,
                             EventAdapterService eventAdapterService,
                             ConfigurationInformation configurationInformation,
                             String statementName,
                             boolean hasSubquery,
                             boolean hasTableAccess)
        throws IllegalArgumentException
    {
        super(lookupable, filterOperator);
        if (filterOperator != FilterOperator.BOOLEAN_EXPRESSION)
        {
            throw new IllegalArgumentException("Invalid filter operator for filter expression node");
        }
        this.exprNode = exprNode;
        this.taggedEventTypes = taggedEventTypes;
        this.arrayEventTypes = arrayEventTypes;
        this.variableService = variableService;
        this.tableService = tableService;
        this.eventAdapterService = eventAdapterService;
        this.useLargeThreadingProfile = configurationInformation.getEngineDefaults().getExecution().getThreadingProfile() == ConfigurationEngineDefaults.ThreadingProfile.LARGE;
        this.hasFilterStreamSubquery = hasSubquery;
        this.hasTableAccess = hasTableAccess;

        ExprNodeVariableVisitor visitor = new ExprNodeVariableVisitor();
        exprNode.accept(visitor);
        this.hasVariable = visitor.isHasVariables();
    }

    /**
     * Returns the expression node of the boolean expression this filter parameter represents.
     * @return expression node
     */
    public ExprNode getExprNode()
    {
        return exprNode;
    }

    /**
     * Returns the map of tag/stream names to event types that the filter expressions map use (for patterns)
     * @return map
     */
    public LinkedHashMap<String, Pair<EventType, String>> getTaggedEventTypes()
    {
        return taggedEventTypes;
    }

    public final ExprNodeAdapterBase getFilterValue(MatchedEventMap matchedEvents, ExprEvaluatorContext exprEvaluatorContext)
    {
        EventBean[] events = null;

        if ((taggedEventTypes != null && !taggedEventTypes.isEmpty()) || (arrayEventTypes != null && !arrayEventTypes.isEmpty()))
        {
            int size = 0;
            size += (taggedEventTypes != null) ? taggedEventTypes.size() : 0;
            size += (arrayEventTypes != null) ? arrayEventTypes.size() : 0;
            events = new EventBean[size + 1];

            int count = 1;
            if (taggedEventTypes != null)
            {
                for (String tag : taggedEventTypes.keySet())
                {
                    events[count] = matchedEvents.getMatchingEventByTag(tag);
                    count++;
                }
            }

            if (arrayEventTypes != null)
            {
                for (Map.Entry<String, Pair<EventType, String>> entry : arrayEventTypes.entrySet())
                {
                    EventType compositeEventType = entry.getValue().getFirst();
                    events[count] = eventAdapterService.adapterForTypedMap(matchedEvents.getMatchingEventsAsMap(), compositeEventType);
                    count++;
                }
            }
        }

        // handle table evaluator context
        if (hasTableAccess) {
            exprEvaluatorContext = new ExprEvaluatorContextWTableAccess(exprEvaluatorContext, tableService);
        }

        // non-pattern case
        ExprNodeAdapterBase adapter;
        if (events == null) {

            // if a subquery is present in a filter stream acquire the agent instance lock
            if (hasFilterStreamSubquery) {
                adapter = new ExprNodeAdapterBaseStmtLock(filterSpecId, filterSpecParamPathNum, exprNode, exprEvaluatorContext, variableService);
            }
            // no-variable no-prior event evaluation
            else if (!hasVariable) {
                adapter = new ExprNodeAdapterBase(filterSpecId, filterSpecParamPathNum, exprNode, exprEvaluatorContext);
            }
            else {
                // with-variable no-prior event evaluation
                adapter = new ExprNodeAdapterBaseVariables(filterSpecId, filterSpecParamPathNum, exprNode, exprEvaluatorContext, variableService);
            }
        }
        else {
            // pattern cases
            VariableService variableServiceToUse = hasVariable == false ? null : variableService;
            if (useLargeThreadingProfile) {
                // no-threadlocal evaluation
                // if a subquery is present in a pattern filter acquire the agent instance lock
                if (hasFilterStreamSubquery) {
                    adapter = new ExprNodeAdapterMultiStreamNoTLStmtLock(filterSpecId, filterSpecParamPathNum, exprNode, exprEvaluatorContext, variableServiceToUse, events);
                }
                else {
                    adapter = new ExprNodeAdapterMultiStreamNoTL(filterSpecId, filterSpecParamPathNum, exprNode, exprEvaluatorContext, variableServiceToUse, events);
                }
            }
            else {
                if (hasFilterStreamSubquery) {
                    adapter = new ExprNodeAdapterMultiStreamStmtLock(filterSpecId, filterSpecParamPathNum, exprNode, exprEvaluatorContext, variableServiceToUse, events);
                }
                else {
                    // evaluation with threadlocal cache
                    adapter = new ExprNodeAdapterMultiStream(filterSpecId, filterSpecParamPathNum, exprNode, exprEvaluatorContext, variableServiceToUse, events);
                }
            }
        }

        if (!hasTableAccess) {
            return adapter;
        }

        // handle table
        return new ExprNodeAdapterBaseWTableAccess(filterSpecId, filterSpecParamPathNum, exprNode, exprEvaluatorContext, adapter, tableService);
    }

    public final String toString()
    {
        return super.toString() + "  exprNode=" + exprNode.toString();
    }

    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        if (!(obj instanceof FilterSpecParamExprNode))
        {
            return false;
        }

        FilterSpecParamExprNode other = (FilterSpecParamExprNode) obj;
        if (!super.equals(other))
        {
            return false;
        }

        if (exprNode != other.exprNode)
        {
            return false;
        }

        return true;
    }

    public int hashCode()
    {
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
}
