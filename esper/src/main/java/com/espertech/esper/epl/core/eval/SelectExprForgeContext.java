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
package com.espertech.esper.epl.core.eval;

import com.espertech.esper.epl.expression.core.ExprForge;
import com.espertech.esper.event.EventAdapterService;

public class SelectExprForgeContext {
    private final ExprForge[] exprForges;
    private final String[] columnNames;
    private final EventAdapterService eventAdapterService;
    private final int numStreams;

    public SelectExprForgeContext(ExprForge[] exprForges, String[] columnNames, EventAdapterService eventAdapterService, int numStreams) {
        this.exprForges = exprForges;
        this.columnNames = columnNames;
        this.eventAdapterService = eventAdapterService;
        this.numStreams = numStreams;
    }

    public ExprForge[] getExprForges() {
        return exprForges;
    }

    public String[] getColumnNames() {
        return columnNames;
    }

    public EventAdapterService getEventAdapterService() {
        return eventAdapterService;
    }

    public int getNumStreams() {
        return numStreams;
    }
}
