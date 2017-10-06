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
package com.espertech.esper.epl.table.merge;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.core.service.EPStatementHandle;
import com.espertech.esper.core.service.InternalEventRouteDest;
import com.espertech.esper.core.service.InternalEventRouter;
import com.espertech.esper.epl.agg.service.common.AggregationRowPair;
import com.espertech.esper.epl.core.select.SelectExprProcessor;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.table.mgmt.TableStateInstance;
import com.espertech.esper.epl.table.mgmt.TableStateRowFactory;
import com.espertech.esper.epl.table.onaction.TableOnMergeViewChangeHandler;
import com.espertech.esper.util.AuditPath;

public class TableOnMergeActionIns extends TableOnMergeAction {
    private final SelectExprProcessor insertHelper;
    private final InternalEventRouter internalEventRouter;
    private final EPStatementHandle statementHandle;
    private final InternalEventRouteDest internalEventRouteDest;
    private final boolean audit;
    private final TableStateRowFactory tableStateRowFactory;

    public TableOnMergeActionIns(ExprEvaluator optionalFilter, SelectExprProcessor insertHelper, InternalEventRouter internalEventRouter, EPStatementHandle statementHandle, InternalEventRouteDest internalEventRouteDest, boolean audit, TableStateRowFactory tableStateRowFactory) {
        super(optionalFilter);
        this.insertHelper = insertHelper;
        this.internalEventRouter = internalEventRouter;
        this.statementHandle = statementHandle;
        this.internalEventRouteDest = internalEventRouteDest;
        this.audit = audit;
        this.tableStateRowFactory = tableStateRowFactory;
    }

    public void apply(EventBean matchingEvent, EventBean[] eventsPerStream, TableStateInstance tableStateInstance, TableOnMergeViewChangeHandler changeHandlerAdded, TableOnMergeViewChangeHandler changeHandlerRemoved, ExprEvaluatorContext exprEvaluatorContext) {
        EventBean theEvent = insertHelper.process(eventsPerStream, true, true, exprEvaluatorContext);
        if (internalEventRouter == null) {
            AggregationRowPair aggs = tableStateRowFactory.makeAggs(exprEvaluatorContext.getAgentInstanceId(), null, null, tableStateInstance.getAggregationServicePassThru());
            ((Object[]) theEvent.getUnderlying())[0] = aggs;
            tableStateInstance.addEvent(theEvent);
            if (changeHandlerAdded != null) {
                changeHandlerAdded.add(theEvent, eventsPerStream, true, exprEvaluatorContext);
            }
            return;
        }

        if (audit) {
            AuditPath.auditInsertInto(internalEventRouteDest.getEngineURI(), statementHandle.getStatementName(), theEvent);
        }
        internalEventRouter.route(theEvent, statementHandle, internalEventRouteDest, exprEvaluatorContext, false);
    }

    public String getName() {
        return internalEventRouter != null ? "insert-into" : "select";
    }

    public boolean isInsertIntoBinding() {
        return internalEventRouter == null;
    }
}
