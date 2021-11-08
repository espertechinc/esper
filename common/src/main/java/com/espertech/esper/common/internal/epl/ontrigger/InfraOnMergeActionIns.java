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
package com.espertech.esper.common.internal.epl.ontrigger;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.collection.OneEventCollection;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.agg.core.AggregationRow;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityEvaluate;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprProcessor;
import com.espertech.esper.common.internal.epl.table.core.Table;
import com.espertech.esper.common.internal.epl.table.core.TableInstance;

public class InfraOnMergeActionIns extends InfraOnMergeAction {
    public final static EPTypeClass EPTYPE = new EPTypeClass(InfraOnMergeActionIns.class);

    private final SelectExprProcessor insertHelper;
    private final Table insertIntoTable;
    private final boolean audit;
    private final boolean route;
    private final ExprEvaluator eventPrecedence;

    public InfraOnMergeActionIns(ExprEvaluator optionalFilter, SelectExprProcessor insertHelper, Table insertIntoTable, boolean audit, boolean route, ExprEvaluator eventPrecedence) {
        super(optionalFilter);
        this.insertHelper = insertHelper;
        this.insertIntoTable = insertIntoTable;
        this.audit = audit;
        this.route = route;
        this.eventPrecedence = eventPrecedence;
    }

    public void apply(EventBean matchingEvent, EventBean[] eventsPerStream, OneEventCollection newData, OneEventCollection oldData, AgentInstanceContext agentInstanceContext) {
        EventBean theEvent = insertHelper.process(eventsPerStream, true, true, agentInstanceContext);

        if (insertIntoTable != null) {
            TableInstance tableInstance = insertIntoTable.getTableInstance(agentInstanceContext.getAgentInstanceId());
            tableInstance.addEventUnadorned(theEvent);
            return;
        }

        if (!route) {
            newData.add(theEvent);
            return;
        }

        if (audit) {
            agentInstanceContext.getAuditProvider().insert(theEvent, agentInstanceContext);
        }

        int precedence = 0;
        if (eventPrecedence != null) {
            Integer result = (Integer) eventPrecedence.evaluate(new EventBean[] {theEvent}, true, agentInstanceContext);
            if (result != null) {
                precedence = result;
            }
        }

        agentInstanceContext.getInternalEventRouter().route(theEvent, agentInstanceContext, false, precedence);
    }

    public void apply(EventBean matchingEvent, EventBean[] eventsPerStream, TableInstance tableStateInstance, OnExprViewTableChangeHandler changeHandlerAdded, OnExprViewTableChangeHandler changeHandlerRemoved, AgentInstanceContext agentInstanceContext) {
        EventBean theEvent = insertHelper.process(eventsPerStream, true, true, agentInstanceContext);
        if (!route) {
            AggregationRow aggs = tableStateInstance.getTable().getAggregationRowFactory().make();
            ((Object[]) theEvent.getUnderlying())[0] = aggs;
            tableStateInstance.addEvent(theEvent);
            if (changeHandlerAdded != null) {
                changeHandlerAdded.add(theEvent, eventsPerStream, true, agentInstanceContext);
            }
            return;
        }

        if (insertIntoTable != null) {
            TableInstance tableInstance = insertIntoTable.getTableInstance(agentInstanceContext.getAgentInstanceId());
            tableInstance.addEventUnadorned(theEvent);
            return;
        }

        if (audit) {
            agentInstanceContext.getAuditProvider().insert(theEvent, agentInstanceContext);
        }

        // Evaluate event precedence
        int precedence = ExprNodeUtilityEvaluate.evaluateIntOptional(eventPrecedence, theEvent, 0, agentInstanceContext);

        agentInstanceContext.getInternalEventRouter().route(theEvent, agentInstanceContext, false, precedence);
    }

    public String getName() {
        return route ? "insert-into" : "select";
    }
}
