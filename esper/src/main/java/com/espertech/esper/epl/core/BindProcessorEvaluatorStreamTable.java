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
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.table.mgmt.TableMetadata;

public class BindProcessorEvaluatorStreamTable implements ExprEvaluator {
    private final int streamNum;
    private final Class returnType;
    private final TableMetadata tableMetadata;

    public BindProcessorEvaluatorStreamTable(int streamNum, Class returnType, TableMetadata tableMetadata) {
        this.streamNum = streamNum;
        this.returnType = returnType;
        this.tableMetadata = tableMetadata;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        EventBean theEvent = eventsPerStream[streamNum];
        if (theEvent != null) {
            return tableMetadata.getEventToPublic().convertToUnd(theEvent, eventsPerStream, isNewData, exprEvaluatorContext);
        }
        return null;
    }

    public Class getType() {
        return returnType;
    }
}
