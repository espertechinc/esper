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
 * Processor for select-clause expressions that handles wildcards for single streams with no insert-into.
 */
public class SelectExprWildcardTableProcessor implements SelectExprProcessor {
    private final TableMetadata metadata;

    public SelectExprWildcardTableProcessor(String tableName, TableService tableService) {
        metadata = tableService.getTableMetadata(tableName);
    }

    public EventBean process(EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvaluatorContext) {
        EventBean event = eventsPerStream[0];
        if (event == null) {
            return null;
        }
        return metadata.getPublicEventBean(event, eventsPerStream, isNewData, exprEvaluatorContext);
    }

    public EventType getResultEventType() {
        return metadata.getPublicEventType();
    }
}
