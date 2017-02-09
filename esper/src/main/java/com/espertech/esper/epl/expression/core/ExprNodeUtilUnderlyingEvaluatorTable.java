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
package com.espertech.esper.epl.expression.core;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.table.mgmt.TableMetadata;

public class ExprNodeUtilUnderlyingEvaluatorTable implements ExprEvaluator {
    private final int streamNum;
    private final Class resultType;
    private final TableMetadata tableMetadata;

    public ExprNodeUtilUnderlyingEvaluatorTable(int streamNum, Class resultType, TableMetadata tableMetadata) {
        this.streamNum = streamNum;
        this.resultType = resultType;
        this.tableMetadata = tableMetadata;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        if (eventsPerStream == null) {
            return null;
        }
        EventBean event = eventsPerStream[streamNum];
        if (event == null) {
            return null;
        }
        return tableMetadata.getEventToPublic().convertToUnd(event, eventsPerStream, isNewData, context);
    }

    public Class getType() {
        return resultType;
    }
}
