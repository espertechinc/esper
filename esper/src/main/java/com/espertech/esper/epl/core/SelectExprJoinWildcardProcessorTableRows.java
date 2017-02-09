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
package com.espertech.esper.epl.core;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.table.mgmt.TableMetadata;
import com.espertech.esper.epl.table.mgmt.TableService;

/**
 * Processor for select-clause expressions that handles wildcards. Computes results based on matching events.
 */
public class SelectExprJoinWildcardProcessorTableRows implements SelectExprProcessor {
    private final SelectExprProcessor inner;
    private final EventBean[] eventsPerStreamWTableRows;
    private final TableMetadata[] tables;

    public SelectExprJoinWildcardProcessorTableRows(EventType[] types, SelectExprProcessor inner, TableService tableService) {
        this.inner = inner;
        eventsPerStreamWTableRows = new EventBean[types.length];
        tables = new TableMetadata[types.length];
        for (int i = 0; i < types.length; i++) {
            tables[i] = tableService.getTableMetadataFromEventType(types[i]);
        }
    }

    public EventBean process(EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvaluatorContext) {
        for (int i = 0; i < eventsPerStreamWTableRows.length; i++) {
            if (tables[i] != null && eventsPerStream[i] != null) {
                eventsPerStreamWTableRows[i] = tables[i].getEventToPublic().convert(eventsPerStream[i], eventsPerStream, isNewData, exprEvaluatorContext);
            } else {
                eventsPerStreamWTableRows[i] = eventsPerStream[i];
            }
        }
        return inner.process(eventsPerStreamWTableRows, isNewData, isSynthesize, exprEvaluatorContext);
    }

    public EventType getResultEventType() {
        return inner.getResultEventType();
    }
}
