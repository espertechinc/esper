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
package com.espertech.esper.filter;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.table.mgmt.TableService;

public class ExprNodeAdapterBaseWTableAccess extends ExprNodeAdapterBase {
    private final ExprNodeAdapterBase evalBase;
    private final TableService tableService;

    public ExprNodeAdapterBaseWTableAccess(int filterSpecId, int filterSpecParamPathNum, ExprNode exprNode, ExprEvaluatorContext evaluatorContext, ExprNodeAdapterBase evalBase, TableService tableService) {
        super(filterSpecId, filterSpecParamPathNum, exprNode, evaluatorContext);
        this.evalBase = evalBase;
        this.tableService = tableService;
    }

    @Override
    public boolean evaluate(EventBean theEvent) {
        try {
            return evalBase.evaluate(theEvent);
        } finally {
            tableService.getTableExprEvaluatorContext().releaseAcquiredLocks();
        }
    }
}
